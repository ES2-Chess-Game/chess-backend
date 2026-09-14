package br.uff.chess.service.exceptions;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String id) {
        super("Partida não encontrada: " + id);
    }
}
