package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.Position;

/**
 * 1 casa para frente (com a casa vazia), 2 casas na primeira jogada
 * (ambas vazias), captura na diagonal (1 casa, peça adversária).
 * Não implementa en passant nem promoção — fora do escopo atual.
 */
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
            return isValidCapture(board, to, color);
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

    private boolean isValidCapture(Board board, Position to, Color color) {
        Piece target = board.get(to.row(), to.col());
        return target != null && target.color() != color;
    }
}
