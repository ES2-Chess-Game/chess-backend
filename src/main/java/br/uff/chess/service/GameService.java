package br.uff.chess.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;
import br.uff.chess.service.rules.SpecialMoves;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private final CheckDetector checkDetector;
    private final DefaultLegalMoveGenerator legalMoveGenerator;

    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator) {
        this.checkDetector = checkDetector;
        this.legalMoveGenerator = legalMoveGenerator;
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

    /**
     * @param promotion peça escolhida na promoção do peão (null = rainha); deve ser
     *                  null em lances que não promovem.
     */
    public Game move(String id, Position from, Position to, PieceType promotion) {
        Game game = getGame(id);

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

        boolean castling = SpecialMoves.isCastlingAttempt(piece, from, to);
        boolean enPassant = SpecialMoves.isEnPassant(game, piece, from, to);
        if (castling) {
            if (!SpecialMoves.canCastle(game, from, to)) {
                throw new IllegalMoveException("Roque ilegal");
            }
        } else if (enPassant) {
            if (enPassantExposesKing(board, piece, from, to)) {
                throw new IllegalMoveException("Movimento ilegal ou deixa o próprio rei em xeque");
            }
        } else if (!legalMoveGenerator.isLegalMove(board, from, to, piece.color())) {
            throw new IllegalMoveException("Movimento ilegal ou deixa o próprio rei em xeque");
        }
        PieceType promoted = SpecialMoves.resolvePromotion(piece, to, promotion);

        SpecialMoves.apply(game, piece, from, to, castling, enPassant, promoted);
        game.alternarTurno();
        game.setStatus(checkDetector.isKingInCheck(board, game.getTurnoAtual())
                ? GameStatus.XEQUE : GameStatus.EM_ANDAMENTO);
        return game;
    }

    /** Simula o en passant em uma cópia do tabuleiro e verifica se o próprio rei ficaria em xeque. */
    private boolean enPassantExposesKing(Board board, Piece pawn, Position from, Position to) {
        Board simulated = new Board();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                simulated.set(row, col, board.get(row, col));
            }
        }
        simulated.set(from.row(), to.col(), null); // peão capturado fica ao lado da origem
        simulated.set(from, null);
        simulated.set(to, pawn);
        return checkDetector.isKingInCheck(simulated, pawn.color());
    }
}
