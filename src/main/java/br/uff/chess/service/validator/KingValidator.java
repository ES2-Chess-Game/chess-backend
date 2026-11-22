package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Position;

public class KingValidator implements PieceMoveValidator {

    @Override
    public boolean isValid(Board board, Position from, Position to, Color color) {
        int dr = Math.abs(to.row() - from.row());
        int dc = Math.abs(to.col() - from.col());
        if (dr == 0 && dc == 0) {
            return false;
        }
        boolean oneSquare = dr <= 1 && dc <= 1;
        return oneSquare && !ValidatorUtils.hasOwnPieceAt(board, to, color);
    }
}
