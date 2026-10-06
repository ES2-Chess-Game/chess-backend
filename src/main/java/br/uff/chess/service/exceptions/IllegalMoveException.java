package br.uff.chess.service.exceptions;

public class IllegalMoveException extends RuntimeException {

    public IllegalMoveException(String message) {
        super(message);
    }
}
