package br.uff.chess.service.rules;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

/**
 * Detecta se uma casa está sob ataque de uma cor. Usado pelo roque (o rei não
 * pode sair, passar nem chegar a uma casa atacada).
 */
public final class AttackedSquares {

    private static final int[][] KNIGHT = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
    private static final int[][] KING = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
    private static final int[][] ORTHOGONAL = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] DIAGONAL = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private AttackedSquares() {
    }

    public static boolean isAttacked(Board board, Position square, Color by) {
        return attackedByPawn(board, square, by)
                || attackedByJumper(board, square, by, KNIGHT, PieceType.CAVALO)
                || attackedByJumper(board, square, by, KING, PieceType.REI)
                || attackedBySlider(board, square, by, ORTHOGONAL, PieceType.TORRE)
                || attackedBySlider(board, square, by, DIAGONAL, PieceType.BISPO);
    }

    private static boolean attackedByPawn(Board board, Position square, Color by) {
        // Peão branco anda para linhas menores, então ataca a partir da linha logo abaixo (row + 1).
        int pawnRow = square.row() + (by == Color.BRANCA ? 1 : -1);
        return hasPiece(board, pawnRow, square.col() - 1, by, PieceType.PEAO)
                || hasPiece(board, pawnRow, square.col() + 1, by, PieceType.PEAO);
    }

    private static boolean attackedByJumper(Board board, Position square, Color by, int[][] offsets, PieceType type) {
        for (int[] o : offsets) {
            if (hasPiece(board, square.row() + o[0], square.col() + o[1], by, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean attackedBySlider(Board board, Position square, Color by, int[][] dirs, PieceType type) {
        for (int[] d : dirs) {
            int row = square.row() + d[0];
            int col = square.col() + d[1];
            while (Board.isInside(row, col)) {
                Piece piece = board.get(row, col);
                if (piece != null) {
                    if (piece.color() == by && (piece.type() == type || piece.type() == PieceType.RAINHA)) {
                        return true;
                    }
                    break;
                }
                row += d[0];
                col += d[1];
            }
        }
        return false;
    }

    private static boolean hasPiece(Board board, int row, int col, Color color, PieceType type) {
        if (!Board.isInside(row, col)) {
            return false;
        }
        Piece piece = board.get(row, col);
        return piece != null && piece.color() == color && piece.type() == type;
    }
}
