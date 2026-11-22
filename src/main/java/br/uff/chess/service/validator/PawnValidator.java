package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

public class PawnValidator implements PieceMoveValidator {

    @Override
    public boolean isValid(Board board, Position from, Position to, Color color) {
        int direction = color == Color.BRANCA ? -1 : 1;
        int startRow = color == Color.BRANCA ? 6 : 1;
        int dRow = to.row() - from.row();
        int dCol = to.col() - from.col();

        if (dCol == 0) {
            return isValidForwardMove(board, from, to, direction, startRow, dRow);
        }
        if (Math.abs(dCol) == 1 && dRow == direction) {
            return isValidCapture(board, from, to, color);
        }
        return false;
    }

    private boolean isValidForwardMove(Board board, Position from, Position to, int direction, int startRow, int dRow) {
        if (board.get(to.row(), to.col()) != null) {
            return false; // peão não captura andando reto
        }
        if (dRow == direction) {
            return true;
        }
        if (from.row() == startRow && dRow == 2 * direction) {
            int middleRow = from.row() + direction;
            return board.get(middleRow, from.col()) == null;
        }
        return false;
    }

    private boolean isValidCapture(Board board, Position from, Position to, Color color) {
        Piece target = board.get(to.row(), to.col());
        if (target != null) {
            return target.color() != color;
        }

        Piece adjacentPawn = board.get(from.row(), to.col());
        return to.equals(board.getEnPassantTarget()) && adjacentPawn != null &&
                adjacentPawn.type() == PieceType.PEAO &&
                adjacentPawn.color() != color;
    }
}
