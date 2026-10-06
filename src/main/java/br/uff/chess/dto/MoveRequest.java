package br.uff.chess.dto;

import br.uff.chess.model.Position;

public record MoveRequest(int origemLinha, int origemColuna, int destinoLinha, int destinoColuna) {

    public Position origem() {
        return new Position(origemLinha, origemColuna);
    }

    public Position destino() {
        return new Position(destinoLinha, destinoColuna);
    }
}
