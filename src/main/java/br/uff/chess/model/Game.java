package br.uff.chess.model;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private final String id;
    private final Board board;
    private final CastlingRights castlingRights = new CastlingRights();
    private Position enPassantTarget;
    private Color turnoAtual;
    private GameStatus status;
    private Color vencedor;
    private Long userId;
    private String visitorId;
    private GameSettings settings;
    private final List<PlayedMove> moves = new ArrayList<>();
    private long createdAt = System.currentTimeMillis();
    private Long finishedAt;
    private long turnStartedAt;
    private long whiteMs;
    private long blackMs;

    public Game(String id, Board board, Color turnoAtual) {
        this.id = id;
        this.board = board;
        this.turnoAtual = turnoAtual;
        this.status = GameStatus.EM_ANDAMENTO;
    }

    public String getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public Color getTurnoAtual() {
        return turnoAtual;
    }

    public CastlingRights getCastlingRights() {
        return castlingRights;
    }

    /** Casa "pulada" por um peão que acabou de avançar duas casas; null se não houver. */
    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(Position enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Color getVencedor() {
        return vencedor;
    }

    public void setVencedor(Color vencedor) {
        this.vencedor = vencedor;
    }

    public boolean isFinalizada() {
        return status == GameStatus.XEQUE_MATE || status == GameStatus.EMPATE
                || status == GameStatus.TEMPO_ESGOTADO || status == GameStatus.DESISTENCIA;
    }

    public void configure(GameSettings settings, String visitorId, long now) {
        this.settings = settings;
        this.visitorId = visitorId;
        this.createdAt = now;
        this.turnStartedAt = now;
        this.whiteMs = settings.durationMs();
        this.blackMs = settings.durationMs();
    }

    public GameSettings getSettings() {
        return settings;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Long getFinishedAt() {
        return finishedAt;
    }

    public long getTurnStartedAt() {
        return turnStartedAt;
    }

    public List<PlayedMove> getMoves() {
        return List.copyOf(moves);
    }

    public void recordMove(PlayedMove move) {
        moves.add(move);
    }

    public long clockBalance(Color color) {
        return color == Color.BRANCA ? whiteMs : blackMs;
    }

    public Long clockRemaining(Color color, long now) {
        if (settings == null || settings.durationMs() == 0) {
            return null;
        }
        long balance = clockBalance(color);
        return color == turnoAtual && !isFinalizada()
                ? Math.max(0, balance - Math.max(0, now - turnStartedAt)) : balance;
    }

    public boolean expire(long now) {
        Long remaining = clockRemaining(turnoAtual, now);
        if (isFinalizada() || remaining == null || remaining != 0) {
            return false;
        }
        end(GameStatus.TEMPO_ESGOTADO, opposite(turnoAtual), now);
        return true;
    }

    public void end(GameStatus reason, Color winner, long now) {
        debitClock(now);
        status = reason;
        vencedor = winner;
        finishedAt = now;
    }

    public void advanceTurn(long now) {
        debitClock(now);
        alternarTurno();
        if (settings != null && !"match".equals(settings.tempo())) {
            whiteMs = settings.durationMs();
            blackMs = settings.durationMs();
        }
        turnStartedAt = now;
    }

    private void debitClock(long now) {
        Long remaining = clockRemaining(turnoAtual, now);
        if (remaining == null) {
            return;
        }
        if (turnoAtual == Color.BRANCA) {
            whiteMs = remaining;
        } else {
            blackMs = remaining;
        }
    }

    public void restoreTimes(long created, Long finished, long turnStarted, long white, long black) {
        createdAt = created;
        finishedAt = finished;
        turnStartedAt = turnStarted;
        whiteMs = white;
        blackMs = black;
    }

    public static Color opposite(Color color) {
        return color == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }

    public void alternarTurno() {
        turnoAtual = turnoAtual == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }
}
