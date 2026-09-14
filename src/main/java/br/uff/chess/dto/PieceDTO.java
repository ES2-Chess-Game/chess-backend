package br.uff.chess.dto;

import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;

public record PieceDTO(PieceType tipo, Color cor) {

    public static PieceDTO from(Piece piece) {
        return new PieceDTO(piece.type(), piece.color());
    }
}
