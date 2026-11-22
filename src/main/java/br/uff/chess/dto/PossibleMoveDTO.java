package br.uff.chess.dto;

import br.uff.chess.model.Position;

public record PossibleMoveDTO(int linha, int coluna, int row, int col) {
    public static PossibleMoveDTO from(Position p) {
        return new PossibleMoveDTO(p.row(), p.col(), p.row(), p.col());
    }
}
