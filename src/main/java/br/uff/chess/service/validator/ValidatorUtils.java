package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.Position;

/**
 * Helpers compartilhados pelos validadores de linha reta e diagonal
 * (Torre, Bispo e Rainha).
 */
final class ValidatorUtils {

    private ValidatorUtils() {
    }

    static boolean hasOwnPieceAt(Board board, Position to, Color color) {
        Piece target = board.get(to.row(), to.col());
        return target != null && target.color() == color;
    }

    /**
     * Assume que from -> to já é uma reta ortogonal ou diagonal válida.
     * Verifica se todas as casas entre elas (exclusive) estão vazias.
     */
    static boolean pathIsClear(Board board, Position from, Position to) {
        int dRow = Integer.signum(to.row() - from.row());
        int dCol = Integer.signum(to.col() - from.col());

        int row = from.row() + dRow;
        int col = from.col() + dCol;
        while (row != to.row() || col != to.col()) {
            if (board.get(row, col) != null) {
                return false;
            }
            row += dRow;
            col += dCol;
        }
        return true;
    }
}
