package br.uff.chess.service.exceptions;

public class AvatarNotFoundException extends RuntimeException {

    public AvatarNotFoundException(Long id) {
        super("Avatar não encontrado: " + id);
    }
}
