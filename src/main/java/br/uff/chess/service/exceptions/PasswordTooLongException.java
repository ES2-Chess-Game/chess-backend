package br.uff.chess.service.exceptions;

public class PasswordTooLongException extends RuntimeException {

    public PasswordTooLongException() {
        super("A senha deve ter até 72 bytes em UTF-8");
    }
}
