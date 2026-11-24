package br.uff.chess.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameResults;
import br.uff.chess.model.GameSettings;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.GameSummary;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PlayedMove;
import br.uff.chess.model.Position;
import tools.jackson.databind.ObjectMapper;

@Repository
public class GameStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public GameStore(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void insert(Game game) {
        jdbc.update("""
                INSERT INTO chess_game (id, user_id, visitor_id, status, created_at, finished_at, state, mode, winner, settings)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, game.getId(), game.getUserId(), game.getVisitorId(), game.getStatus().name(),
                game.getCreatedAt(), game.getFinishedAt(), encode(game), game.getSettings().modo(), winner(game),
                json.writeValueAsString(game.getSettings()));
    }

    public void update(Game game) {
        jdbc.update("""
                UPDATE chess_game SET status = ?, finished_at = ?, state = ?, mode = ?, winner = ?, settings = ?
                WHERE id = ?
                """, game.getStatus().name(), game.getFinishedAt(), encode(game), game.getSettings().modo(),
                winner(game), json.writeValueAsString(game.getSettings()), game.getId());
    }

    public Optional<Game> find(String id, boolean lock) {
        String sql = "SELECT * FROM chess_game WHERE id = ?";
        return jdbc.query(lock ? sql + " FOR UPDATE" : sql, this::decode, id).stream().findFirst();
    }

    public List<Game> finished(Long userId, String visitorId) {
        if (userId == null && visitorId == null) return List.of();
        return jdbc.query("""
                SELECT * FROM chess_game
                WHERE (user_id = ? OR (user_id IS NULL AND visitor_id = ?)) AND finished_at IS NOT NULL
                ORDER BY finished_at DESC LIMIT 50
                """, this::decode, userId, visitorId);
    }

    public boolean mayAccessById(String id, Long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM chess_game WHERE id = ? AND (user_id IS NULL OR user_id = ?))
                """, Boolean.class, id, userId));
    }

    public List<String> activeIds(Long userId, String visitorId) {
        if (userId == null && visitorId == null) return List.of();
        return jdbc.queryForList("""
                SELECT id FROM chess_game
                WHERE (user_id = ? OR (user_id IS NULL AND visitor_id = ?)) AND finished_at IS NULL
                ORDER BY created_at DESC LIMIT 50
                """, String.class, userId, visitorId);
    }

    public List<GameSummary> historySummaries(Long userId, String visitorId) {
        if (userId == null && visitorId == null) return List.of();
        return jdbc.query("""
                SELECT id, status, created_at, finished_at, winner, settings,
                       CASE WHEN settings IS NULL OR (winner IS NULL AND status <> 'EMPATE')
                            THEN state END AS legacy_state
                FROM chess_game
                WHERE (user_id = ? OR (user_id IS NULL AND visitor_id = ?)) AND finished_at IS NOT NULL
                ORDER BY finished_at DESC LIMIT 50
                """, this::decodeSummary, userId, visitorId);
    }

    public GameResults aiResults(long userId) {
        return jdbc.query("""
                SELECT COUNT(CASE WHEN winner = 'BRANCA' THEN 1 END) AS wins,
                       COUNT(CASE WHEN winner = 'PRETA' THEN 1 END) AS losses,
                       COUNT(CASE WHEN status = 'EMPATE' THEN 1 END) AS draws,
                       CAST(NULL AS VARCHAR) AS status, CAST(NULL AS TEXT) AS legacy_state
                FROM chess_game WHERE user_id = ? AND finished_at IS NOT NULL AND mode = 'ai'
                        AND (winner IS NOT NULL OR status = 'EMPATE')
                UNION ALL
                SELECT 0, 0, 0, status, state
                FROM chess_game WHERE user_id = ? AND finished_at IS NOT NULL
                        AND (mode IS NULL OR (winner IS NULL AND status <> 'EMPATE'))
                """, rows -> {
                    long wins = 0;
                    long losses = 0;
                    long draws = 0;
                    while (rows.next()) {
                        String state = rows.getString("legacy_state");
                        if (state == null) {
                            wins += rows.getLong("wins");
                            losses += rows.getLong("losses");
                            draws += rows.getLong("draws");
                            continue;
                        }
                        Snapshot snapshot = json.readValue(state, Snapshot.class);
                        if (!"ai".equals(snapshot.settings().modo())) continue;
                        if (snapshot.winner() == Color.BRANCA) wins++;
                        if (snapshot.winner() == Color.PRETA) losses++;
                        if (GameStatus.EMPATE.name().equals(rows.getString("status"))) draws++;
                    }
                    return new GameResults(wins, losses, draws);
                }, userId, userId);
    }

    public Map<Long, GameResults> rankingResults() {
        return jdbc.query("""
                SELECT user_id, COUNT(CASE WHEN winner = 'BRANCA' AND status = 'XEQUE_MATE' THEN 1 END) AS wins,
                       COUNT(CASE WHEN winner = 'PRETA' THEN 1 END) AS losses,
                       COUNT(CASE WHEN status = 'EMPATE' THEN 1 END) AS draws,
                       CAST(NULL AS VARCHAR) AS status, CAST(NULL AS TEXT) AS legacy_state
                FROM chess_game WHERE user_id IS NOT NULL AND finished_at IS NOT NULL AND mode = 'ai'
                        AND (winner IS NOT NULL OR status = 'EMPATE')
                GROUP BY user_id
                UNION ALL
                SELECT user_id, 0, 0, 0, status, state
                FROM chess_game WHERE user_id IS NOT NULL AND finished_at IS NOT NULL
                        AND (mode IS NULL OR (mode = 'ai' AND winner IS NULL AND status <> 'EMPATE'))
                """, rows -> {
                    Map<Long, GameResults> scores = new HashMap<>();
                    while (rows.next()) {
                        String state = rows.getString("legacy_state");
                        GameResults row;
                        if (state == null) {
                            row = new GameResults(rows.getLong("wins"), rows.getLong("losses"), rows.getLong("draws"));
                        } else {
                            Snapshot snapshot = json.readValue(state, Snapshot.class);
                            if (!"ai".equals(snapshot.settings().modo())) continue;
                            String status = rows.getString("status");
                            row = new GameResults(snapshot.winner() == Color.BRANCA && "XEQUE_MATE".equals(status) ? 1 : 0,
                                    snapshot.winner() == Color.PRETA ? 1 : 0, "EMPATE".equals(status) ? 1 : 0);
                        }
                        scores.merge(rows.getLong("user_id"), row, (left, right) -> new GameResults(
                                left.vitorias() + right.vitorias(), left.derrotas() + right.derrotas(),
                                left.empates() + right.empates()));
                    }
                    return scores;
                });
    }

    private GameSummary decodeSummary(ResultSet row, int index) throws SQLException {
        String legacyState = row.getString("legacy_state");
        GameSettings settings;
        Color winner;
        if (legacyState != null) {
            Snapshot legacy = json.readValue(legacyState, Snapshot.class);
            settings = legacy.settings();
            winner = legacy.winner();
        } else {
            settings = json.readValue(row.getString("settings"), GameSettings.class);
            String color = row.getString("winner");
            winner = color == null ? null : Color.valueOf(color);
        }
        return new GameSummary(row.getString("id"), GameStatus.valueOf(row.getString("status")), winner,
                settings, row.getLong("created_at"), row.getObject("finished_at", Long.class));
    }

    private String winner(Game game) {
        return game.getVencedor() == null ? null : game.getVencedor().name();
    }

    private String encode(Game game) {
        Piece[][] board = new Piece[Board.SIZE][Board.SIZE];
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board[row][col] = game.getBoard().get(row, col);
            }
        }
        var rights = game.getCastlingRights();
        return json.writeValueAsString(new Snapshot(board, game.getTurnoAtual(), game.getVencedor(),
                game.getEnPassantTarget(), rights.canKingSide(Color.BRANCA), rights.canQueenSide(Color.BRANCA),
                rights.canKingSide(Color.PRETA), rights.canQueenSide(Color.PRETA), game.getSettings(),
                game.getMoves(), game.getTurnStartedAt(), game.clockBalance(Color.BRANCA),
                game.clockBalance(Color.PRETA)));
    }

    private Game decode(ResultSet row, int index) throws SQLException {
        Snapshot state = json.readValue(row.getString("state"), Snapshot.class);
        Board board = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                board.set(r, c, state.board()[r][c]);
            }
        }
        Game game = new Game(row.getString("id"), board, state.turn());
        game.setUserId(row.getObject("user_id", Long.class));
        game.configure(state.settings(), row.getString("visitor_id"), row.getLong("created_at"));
        game.setStatus(GameStatus.valueOf(row.getString("status")));
        game.setVencedor(state.winner());
        game.setEnPassantTarget(state.enPassant());
        var rights = game.getCastlingRights();
        if (!state.whiteKingSide()) rights.revokeForSquare(new Position(7, 7));
        if (!state.whiteQueenSide()) rights.revokeForSquare(new Position(7, 0));
        if (!state.blackKingSide()) rights.revokeForSquare(new Position(0, 7));
        if (!state.blackQueenSide()) rights.revokeForSquare(new Position(0, 0));
        state.moves().forEach(game::recordMove);
        game.restoreTimes(row.getLong("created_at"), row.getObject("finished_at", Long.class),
                state.turnStartedAt(), state.whiteMs(), state.blackMs());
        return game;
    }

    private record Snapshot(Piece[][] board, Color turn, Color winner, Position enPassant, boolean whiteKingSide,
            boolean whiteQueenSide, boolean blackKingSide, boolean blackQueenSide, GameSettings settings,
            List<PlayedMove> moves, long turnStartedAt, long whiteMs, long blackMs) {
    }
}
