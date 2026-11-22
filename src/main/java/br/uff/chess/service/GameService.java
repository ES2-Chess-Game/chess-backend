package br.uff.chess.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.GameSettings;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.model.PlayedMove;
import br.uff.chess.repository.GameStore;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;
import br.uff.chess.service.rules.SpecialMoves;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final GameStore store;

    private final EndGameEvaluator endGameEvaluator;
    private final DefaultLegalMoveGenerator legalMoveGenerator;
    private final CheckDetector checkDetector;
    private final MinimaxAI minimaxAI;

    @Autowired
    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator,
            MinimaxAI minimaxAI, GameStore store) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator, checkDetector, minimaxAI,
                store);
    }

    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator,
            MinimaxAI minimaxAI) {
        this(checkDetector, legalMoveGenerator, minimaxAI, null);
    }

    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator, checkDetector);
    }

    GameService(EndGameEvaluator endGameEvaluator, DefaultLegalMoveGenerator legalMoveGenerator,
            CheckDetector checkDetector) {
        this(endGameEvaluator, legalMoveGenerator, checkDetector, new MinimaxAI(new LegalMoveService()), null);
    }

    private GameService(EndGameEvaluator endGameEvaluator, DefaultLegalMoveGenerator legalMoveGenerator,
            CheckDetector checkDetector, MinimaxAI minimaxAI, GameStore store) {
        this.store = store;
        this.endGameEvaluator = endGameEvaluator;
        this.legalMoveGenerator = legalMoveGenerator;
        this.checkDetector = checkDetector;
        this.minimaxAI = minimaxAI;
    }

    @Transactional
    public Game createGame() {
        return createGame(null);
    }

    @Transactional
    public Game createGame(Long userId) {
        return createGame(userId, null, null);
    }

    @Transactional
    public Game createGame(Long userId, String visitorId, GameSettings settings) {
        Game game = new Game(UUID.randomUUID().toString(), new Board(), Color.BRANCA);
        game.setUserId(userId);
        game.configure(settings == null ? new GameSettings(null, null, null, null, null) : settings,
                visitorId, System.currentTimeMillis());
        if (store == null) {
            games.put(game.getId(), game);
        } else {
            store.insert(game);
        }
        return game;
    }

    @Transactional
    public Game getGame(String id) {
        Game game = load(id, true);
        synchronized (game) {
            if (game.expire(System.currentTimeMillis())) {
                save(game);
            }
            return game;
        }
    }

    private Game load(String id, boolean lock) {
        Game game = store == null ? games.get(id) : store.find(id, lock).orElse(null);
        if (game == null) {
            throw new GameNotFoundException(id);
        }
        return game;
    }

    private void save(Game game) {
        if (store != null) store.update(game);
    }

    private void requireActive(Game game) {
        if (game.expire(System.currentTimeMillis())) {
            save(game);
        }
        if (game.isFinalizada()) {
            throw new GameOverException("A partida já foi encerrada");
        }
    }

    private boolean owns(Game game, Long userId, String visitorId) {
        if (game.getUserId() != null) {
            return Objects.equals(game.getUserId(), userId);
        }
        return visitorId != null && visitorId.equals(game.getVisitorId());
    }

    @Transactional
    public Optional<Game> inProgress(Long userId, String visitorId) {
        if (store == null) return Optional.empty();
        for (Game candidate : store.owned(userId, visitorId, false)) {
            Game game = getGame(candidate.getId());
            if (!game.isFinalizada()) return Optional.of(game);
        }
        return Optional.empty();
    }

    @Transactional
    public List<Game> history(Long userId, String visitorId) {
        if (store == null) return List.of();
        for (Game active : store.owned(userId, visitorId, false)) {
            getGame(active.getId());
        }
        return store.owned(userId, visitorId, true);
    }

    @Transactional
    public Game historyDetail(String id, Long userId, String visitorId) {
        Game game = getGame(id);
        if (!game.isFinalizada() || !owns(game, userId, visitorId)) {
            throw new GameNotFoundException(id);
        }
        return game;
    }

    @Transactional
    public Game resign(String id, Color loser, Long userId, String visitorId) {
        Game game = load(id, true);
        if (!owns(game, userId, visitorId)) throw new GameNotFoundException(id);
        if (game.expire(System.currentTimeMillis())) {
            save(game);
        } else if (!game.isFinalizada()) {
            game.end(GameStatus.DESISTENCIA, Game.opposite(loser), System.currentTimeMillis());
            save(game);
        }
        return game;
    }

    @Transactional(noRollbackFor = GameOverException.class)
    public Game move(String id, Position from, Position to) {
        return move(id, from, to, null);
    }

    @Transactional(noRollbackFor = GameOverException.class)
    public Game move(String id, Position from, Position to, PieceType promotion) {
        Game game = load(id, true);
        synchronized (game) {
            return move(game, from, to, promotion);
        }
    }

    private Game move(Game game, Position from, Position to, PieceType promotion) {
        requireActive(game);

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

        Piece captured = enPassant ? board.get(from.row(), to.col()) : board.get(to);
        SpecialMoves.apply(game, piece, from, to, castling, enPassant, promoted);
        game.recordMove(new PlayedMove(from, to, piece.color(), piece.type(), promoted,
                captured == null ? null : captured.type()));
        long now = System.currentTimeMillis();
        game.advanceTurn(now);

        EndGameResult result = endGameEvaluator.evaluate(board, game.getTurnoAtual());
        if (result.status() == GameStatus.XEQUE_MATE || result.status() == GameStatus.EMPATE) {
            game.end(result.status(), result.vencedor(), now);
        } else {
            game.setStatus(result.status());
            if (result.vencedor() != null) game.setVencedor(result.vencedor());
        }
        save(game);
        return game;
    }

    @Transactional(noRollbackFor = GameOverException.class)
    public Game playAiMove(String id) {
        return playAiMove(id, AiDifficulty.ADVANCED);
    }

    @Transactional(noRollbackFor = GameOverException.class)
    public Game playAiMove(String id, AiDifficulty difficulty) {
        Game game = load(id, true);
        synchronized (game) {
            requireActive(game);
            AiDifficulty configured = game.getSettings().dificuldade();
            var move = minimaxAI.chooseMove(game.getBoard(), game.getTurnoAtual(),
                    configured == null ? difficulty : configured);
            if (move.isEmpty()) {
                EndGameResult result = endGameEvaluator.evaluate(game.getBoard(), game.getTurnoAtual());
                if (result.status() == GameStatus.EM_ANDAMENTO || result.status() == GameStatus.XEQUE) {
                    throw new IllegalMoveException("A IA não encontrou um lance legal para esta posição");
                }
                game.end(result.status(), result.vencedor(), System.currentTimeMillis());
                save(game);
                return game;
            }

            return move(game, move.get().from(), move.get().to(), null);
        }
    }

    @Transactional
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

    @Transactional
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
