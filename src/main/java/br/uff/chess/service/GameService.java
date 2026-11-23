package br.uff.chess.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;
import br.uff.chess.service.rules.SpecialMoves;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private final EndGameEvaluator endGameEvaluator;
    private final DefaultLegalMoveGenerator legalMoveGenerator;
    private final CheckDetector checkDetector;
    private final MinimaxAI minimaxAI;

    @Autowired
    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator,
            MinimaxAI minimaxAI) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator, checkDetector, minimaxAI);
    }

    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator, checkDetector);
    }

    GameService(EndGameEvaluator endGameEvaluator, DefaultLegalMoveGenerator legalMoveGenerator,
            CheckDetector checkDetector) {
        this(endGameEvaluator, legalMoveGenerator, checkDetector, new MinimaxAI(new LegalMoveService()));
    }

    private GameService(EndGameEvaluator endGameEvaluator, DefaultLegalMoveGenerator legalMoveGenerator,
            CheckDetector checkDetector, MinimaxAI minimaxAI) {
        this.endGameEvaluator = endGameEvaluator;
        this.legalMoveGenerator = legalMoveGenerator;
        this.checkDetector = checkDetector;
        this.minimaxAI = minimaxAI;
    }

    public Game createGame() {
        return createGame(null);
    }

    public Game createGame(Long userId) {
        Game game = new Game(UUID.randomUUID().toString(), new Board(), Color.BRANCA);
        game.setUserId(userId);
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
        synchronized (game) {
            return move(game, from, to, promotion);
        }
    }

    private Game move(Game game, Position from, Position to, PieceType promotion) {
        if (game.isFinalizada()) {
            throw new GameOverException("A partida já foi encerrada");
        }

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
        if (!isLegalMove(game, piece, from, to, castling, enPassant)) {
            throw new IllegalMoveException(castling ? "Roque ilegal" : "Movimento ilegal ou deixa o próprio rei em xeque");
        }
        PieceType promoted = SpecialMoves.resolvePromotion(piece, to, promotion);

        SpecialMoves.apply(game, piece, from, to, castling, enPassant, promoted);
        game.alternarTurno();

        EndGameResult result = endGameEvaluator.evaluate(board, game.getTurnoAtual());
        game.setStatus(result.status());
        if (result.vencedor() != null) {
            game.setVencedor(result.vencedor());
        }
        return game;
    }

    public Game playAiMove(String id) {
        return playAiMove(id, AiDifficulty.ADVANCED);
    }

    public Game playAiMove(String id, AiDifficulty difficulty) {
        Game game = getGame(id);
        synchronized (game) {
            if (game.isFinalizada()) {
                throw new GameOverException("A partida já foi encerrada");
            }

            var move = minimaxAI.chooseMove(game.getBoard(), game.getTurnoAtual(), difficulty);
            if (move.isEmpty()) {
                EndGameResult result = endGameEvaluator.evaluate(game.getBoard(), game.getTurnoAtual());
                game.setStatus(result.status());
                game.setVencedor(result.vencedor());
                if (result.status() == GameStatus.EM_ANDAMENTO) {
                    throw new IllegalMoveException("A IA não encontrou um lance legal para esta posição");
                }
                return game;
            }

            return move(game, move.get().from(), move.get().to(), null);
        }
    }

    public List<LegalMove> getAllLegalMoves(String id) {
        Game game = getGame(id);
        synchronized (game) {
            List<LegalMove> moves = new ArrayList<>();
            if (game.isFinalizada()) {
                return moves;
            }
            for (int row = 0; row < Board.SIZE; row++) {
                for (int col = 0; col < Board.SIZE; col++) {
                    Position from = new Position(row, col);
                    for (Position to : getPossibleMoves(game, from)) {
                        moves.add(new LegalMove(from, to));
                    }
                }
            }
            return moves;
        }
    }

    public List<Position> getPossibleMoves(String id, Position from) {
        if (!Board.isInside(from)) {
            throw new IllegalMoveException("Posição fora do tabuleiro");
        }
        Game game = getGame(id);
        synchronized (game) {
            return game.isFinalizada() ? List.of() : getPossibleMoves(game, from);
        }
    }

    private List<Position> getPossibleMoves(Game game, Position from) {
        Piece piece = game.getBoard().get(from);
        if (piece == null || piece.color() != game.getTurnoAtual()) {
            return List.of();
        }
        List<Position> moves = new ArrayList<>();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Position to = new Position(row, col);
                boolean castling = SpecialMoves.isCastlingAttempt(piece, from, to);
                boolean enPassant = SpecialMoves.isEnPassant(game, piece, from, to);
                if (isLegalMove(game, piece, from, to, castling, enPassant)) {
                    moves.add(to);
                }
            }
        }
        return moves;
    }

    private boolean isLegalMove(Game game, Piece piece, Position from, Position to,
            boolean castling, boolean enPassant) {
        if (castling) {
            return SpecialMoves.canCastle(game, from, to);
        }
        if (enPassant) {
            return !enPassantExposesKing(game.getBoard(), piece, from, to);
        }
        return legalMoveGenerator.isLegalMove(game.getBoard(), from, to, piece.color());
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
