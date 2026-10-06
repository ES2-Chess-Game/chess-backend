package br.uff.chess.model;

public class Game {

    private final String id;
    private final Board board;
    private final CastlingRights castlingRights = new CastlingRights();
    private Position enPassantTarget;
    private Color turnoAtual;
    private GameStatus status;
    private Color vencedor;

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
        return status == GameStatus.XEQUE_MATE || status == GameStatus.EMPATE;
    }

    public void alternarTurno() {
        turnoAtual = turnoAtual == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }
}
