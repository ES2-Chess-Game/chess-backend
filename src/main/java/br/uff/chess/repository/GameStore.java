package br.uff.chess.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameSettings;
import br.uff.chess.model.GameStatus;
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
                INSERT INTO chess_game (id, user_id, visitor_id, status, created_at, finished_at, state)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, game.getId(), game.getUserId(), game.getVisitorId(), game.getStatus().name(),
                game.getCreatedAt(), game.getFinishedAt(), encode(game));
    }

    public void update(Game game) {
        jdbc.update("UPDATE chess_game SET status = ?, finished_at = ?, state = ? WHERE id = ?",
                game.getStatus().name(), game.getFinishedAt(), encode(game), game.getId());
    }

    public Optional<Game> find(String id, boolean lock) {
        String sql = "SELECT * FROM chess_game WHERE id = ?";
        return jdbc.query(lock ? sql + " FOR UPDATE" : sql, this::decode, id).stream().findFirst();
    }

    public List<Game> owned(Long userId, String visitorId, boolean finished) {
        if (userId == null && visitorId == null) {
            return List.of();
        }
        String state = finished ? "finished_at IS NOT NULL" : "finished_at IS NULL";
        String order = finished ? "finished_at DESC" : "created_at DESC";
        return jdbc.query("""
                SELECT * FROM chess_game
                WHERE (user_id = ? OR (user_id IS NULL AND visitor_id = ?)) AND %s
                ORDER BY %s LIMIT 50
                """.formatted(state, order), this::decode, userId, visitorId);
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
