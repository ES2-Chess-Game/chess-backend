package br.uff.chess.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
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

    private static final long TEMPO_POR_JOGADOR_MS = 10 * 60 * 1000L;

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private final EndGameEvaluator endGameEvaluator;
    private final DefaultLegalMoveGenerator legalMoveGenerator;
    private final CheckDetector checkDetector;
    private final MinimaxAI minimaxAI;
    private final Clock clock;

    @Autowired
    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator,
            MinimaxAI minimaxAI, Clock clock) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator, checkDetector, minimaxAI,
                clock);
    }

    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator,
            MinimaxAI minimaxAI) {
        this(checkDetector, legalMoveGenerator, minimaxAI, Clock.systemUTC());
    }

    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator, Clock clock) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator, checkDetector,
                new MinimaxAI(new LegalMoveService()), clock);
    }

    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator, checkDetector);
    }

    GameService(EndGameEvaluator endGameEvaluator, DefaultLegalMoveGenerator legalMoveGenerator,
            CheckDetector checkDetector) {
        this(endGameEvaluator, legalMoveGenerator, checkDetector, new MinimaxAI(new LegalMoveService()),
                Clock.systemUTC());
    }

    private GameService(EndGameEvaluator endGameEvaluator, DefaultLegalMoveGenerator legalMoveGenerator,
            CheckDetector checkDetector, MinimaxAI minimaxAI, Clock clock) {
        this.clock = clock;
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
        game.iniciarRelogio(TEMPO_POR_JOGADOR_MS, clock.instant());
        games.put(game.getId(), game);
        return game;
    }

    public Game getGame(String id) {
        Game game = games.get(id);
        if (game == null) {
            throw new GameNotFoundException(id);
        }
        synchronized (game) {
            game.verificarTempoEsgotado(clock.instant());
        }
        return game;
    }

    public Instant agora() {
        return clock.instant();
    }

    /** Partida em curso (inclusive em xeque) mais recente do usuário. */
    public Optional<Game> findInProgressByUser(Long userId) {
        return games.values().stream()
                .filter(g -> userId.equals(g.getUserId()))
                .filter(g -> !getGame(g.getId()).isFinalizada())
                .max(Comparator.comparing(Game::getCriadaEm));
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
            return applyMove(game, from, to, promotion);
        }
    }

    private Game applyMove(Game game, Position from, Position to, PieceType promotion) {
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
        game.descontarTurno(clock.instant());
        game.alternarTurno();

        EndGameResult result = endGameEvaluator.evaluate(board, game.getTurnoAtual());
        game.setStatus(result.status());
        if (result.vencedor() != null) {
            game.setVencedor(result.vencedor());
        }
        return game;
    }

    public Game playAiMove(String id) {
        Game game = getGame(id);
        if (game.isFinalizada()) {
            throw new GameOverException("A partida já foi encerrada");
        }

        var move = minimaxAI.chooseMove(game.getBoard(), game.getTurnoAtual());
        if (move.isEmpty()) {
            EndGameResult result = endGameEvaluator.evaluate(game.getBoard(), game.getTurnoAtual());
            game.setStatus(result.status());
            game.setVencedor(result.vencedor());
            if (result.status() == GameStatus.EM_ANDAMENTO) {
                throw new IllegalMoveException("A IA não encontrou um lance legal para esta posição");
            }
            return game;
        }

        return move(id, move.get().from(), move.get().to(), null);
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
