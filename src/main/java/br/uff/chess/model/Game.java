package br.uff.chess.model;

public class Game {

    private final String id;
    private final Board board;
    private Color turnoAtual;
    private GameStatus status;

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

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public void alternarTurno() {
        turnoAtual = turnoAtual == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }
}
