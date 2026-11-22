package br.uff.chess.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.model.GameStatus;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private final LegalMoveService legalMoveService;
    private final MinimaxAI minimaxAI;

    public GameService(LegalMoveService legalMoveService, MinimaxAI minimaxAI) {
        this.legalMoveService = legalMoveService;
        this.minimaxAI = minimaxAI;
    }

    public Game createGame() {
        Game game = new Game(UUID.randomUUID().toString(), new Board(), Color.BRANCA);
        games.put(game.getId(), game);
        return game;
    }

    public Game getGame(String id) {
        Game game = games.get(id);
        if (game == null) {
            throw new GameNotFoundException(id);
        }
        return game;
    }

    public Game move(String id, Position from, Position to) {
        return move(id, from, to, null);
    }

    public Game move(String id, Position from, Position to, PieceType promotion) {
        Game game = getGame(id);
        validateOngoingGame(game);

        if (!Board.isInside(from) || !Board.isInside(to)) {
            throw new IllegalMoveException("Posição fora do tabuleiro");
        }
        if (from.equals(to)) {
            throw new IllegalMoveException("Origem e destino são iguais");
        }

        Board board = game.getBoard();
        Piece piece = board.get(from.row(), from.col());
        if (piece == null) {
            throw new IllegalMoveException("Não há peça na posição de origem");
        }
        if (piece.color() != game.getTurnoAtual()) {
            throw new IllegalMoveException("Peça inválida ou não é sua vez");
        }

        boolean promotionMove = piece.type() == PieceType.PEAO &&
                (to.row() == 0 || to.row() == Board.SIZE - 1);
        if (promotionMove) {
            if (promotion == null) {
                promotion = PieceType.RAINHA;
            } else if (!isPromotionPiece(promotion)) {
                throw new IllegalMoveException("A promoção deve ser para rainha, torre, bispo ou cavalo");
            }
        } else if (promotion != null) {
            throw new IllegalMoveException("Promoção só pode ser escolhida ao promover um peão");
        }

        if (!legalMoveService.isLegalMove(board, from, to, piece.color())) {
            throw new IllegalMoveException("Movimento ilegal");
        }

        applyMove(game, from, to, promotion);
        return game;
    }

    public Game playAiMove(String id) {
        return playAiMove(id, AiDifficulty.ADVANCED);
    }

    public Game playAiMove(String id, AiDifficulty difficulty) {
        Game game = getGame(id);
        validateOngoingGame(game);

        Color turn = game.getTurnoAtual();
        var move = minimaxAI.chooseMove(game.getBoard(), turn, difficulty);
        if (move.isEmpty()) {
            updateStatus(game);
            return game;
        }

        applyMove(game, move.get().from(), move.get().to(), move.get().promotion());
        return game;
    }

    public List<Position> getPossibleMoves(String id, Position from) {
        Game game = getGame(id);
        if (!Board.isInside(from)) {
            return List.of();
        }
        Piece piece = game.getBoard().get(from.row(), from.col());
        if (piece == null || piece.color() != game.getTurnoAtual()) {
            return List.of();
        }
        return legalMoveService.getLegalMoves(game.getBoard(), game.getTurnoAtual())
                .stream()
                .filter(move -> move.from().equals(from))
                .map(LegalMove::to)
                .distinct()
                .toList();
    }

    public List<LegalMove> getAllLegalMoves(String id) {
        Game game = getGame(id);
        return legalMoveService.getLegalMoves(game.getBoard(), game.getTurnoAtual());
    }

    private void applyMove(Game game, Position from, Position to, PieceType promotion) {
        LegalMoveService.applyMove(game.getBoard(), from, to, promotion);
        game.alternarTurno();
        updateStatus(game);
    }

    private void updateStatus(Game game) {
        var legalMoves = legalMoveService.getLegalMoves(game.getBoard(), game.getTurnoAtual());
        if (legalMoves.isEmpty()) {
            GameStatus status = legalMoveService.isKingInCheck(game.getBoard(), game.getTurnoAtual())
                    ? GameStatus.XEQUE_MATE
                    : GameStatus.EMPATE;
            game.setStatus(status);
        } else if (legalMoveService.isKingInCheck(game.getBoard(), game.getTurnoAtual())) {
            game.setStatus(GameStatus.XEQUE);
        } else {
            game.setStatus(GameStatus.EM_ANDAMENTO);
        }
    }

    private boolean isPromotionPiece(PieceType pieceType) {
        return pieceType == PieceType.RAINHA || pieceType == PieceType.TORRE ||
                pieceType == PieceType.BISPO || pieceType == PieceType.CAVALO;
    }

    private void validateOngoingGame(Game game) {
        if (game.getStatus() != GameStatus.EM_ANDAMENTO && game.getStatus() != GameStatus.XEQUE) {
            throw new IllegalMoveException("A partida já foi encerrada");
        }
    }
}
