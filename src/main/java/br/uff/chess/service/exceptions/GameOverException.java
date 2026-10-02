package br.uff.chess.service.exceptions;

public class GameOverException extends RuntimeException {

    public GameOverException(String message) {
        super(message);
    }
}
