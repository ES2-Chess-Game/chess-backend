package br.uff.chess.dto;

import br.uff.chess.model.Position;
import br.uff.chess.model.PieceType;

public record MoveRequest(
        int origemLinha,
        int origemColuna,
        int destinoLinha,
        int destinoColuna,
        PieceType promocao) {

    public MoveRequest(int origemLinha, int origemColuna, int destinoLinha, int destinoColuna) {
        this(origemLinha, origemColuna, destinoLinha, destinoColuna, null);
    }

    public Position origem() {
        return new Position(origemLinha, origemColuna);
    }

    public Position destino() {
        return new Position(destinoLinha, destinoColuna);
    }
}
