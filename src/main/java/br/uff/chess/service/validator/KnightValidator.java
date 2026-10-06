package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Position;

public class KnightValidator implements PieceMoveValidator {

    @Override
    public boolean isValid(Board board, Position from, Position to, Color color) {
        int dr = Math.abs(to.row() - from.row());
        int dc = Math.abs(to.col() - from.col());
        boolean lShape = (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
        return lShape && !ValidatorUtils.hasOwnPieceAt(board, to, color);
    }
}
