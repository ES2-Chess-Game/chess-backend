package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Position;

public class RookValidator implements PieceMoveValidator {

    @Override
    public boolean isValid(Board board, Position from, Position to, Color color) {
        if (from.row() != to.row() && from.col() != to.col()) {
            return false;
        }
        return ValidatorUtils.pathIsClear(board, from, to) && !ValidatorUtils.hasOwnPieceAt(board, to, color);
    }
}
