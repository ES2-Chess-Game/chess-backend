package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Position;

public class BishopValidator implements PieceMoveValidator {

    @Override
    public boolean isValid(Board board, Position from, Position to, Color color) {
        int dr = Math.abs(to.row() - from.row());
        int dc = Math.abs(to.col() - from.col());
        if (dr == 0 || dr != dc) {
            return false;
        }
        return ValidatorUtils.pathIsClear(board, from, to) && !ValidatorUtils.hasOwnPieceAt(board, to, color);
    }
}
