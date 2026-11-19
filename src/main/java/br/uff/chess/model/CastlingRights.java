package br.uff.chess.model;

/**
 * Direitos de roque de cada cor. Um direito é perdido (para sempre) quando o rei
 * se move, ou quando a torre daquele lado se move ou é capturada em sua casa de origem.
 */
public class CastlingRights {

    private boolean whiteKingSide = true;
    private boolean whiteQueenSide = true;
    private boolean blackKingSide = true;
    private boolean blackQueenSide = true;

    public boolean canKingSide(Color color) {
        return color == Color.BRANCA ? whiteKingSide : blackKingSide;
    }

    public boolean canQueenSide(Color color) {
        return color == Color.BRANCA ? whiteQueenSide : blackQueenSide;
    }

    public void revokeAll(Color color) {
        if (color == Color.BRANCA) {
            whiteKingSide = false;
            whiteQueenSide = false;
        } else {
            blackKingSide = false;
            blackQueenSide = false;
        }
    }

    /** Revoga o direito associado a uma casa de canto (movimento ou captura da torre). */
    public void revokeForSquare(Position p) {
        if (p.row() == 7 && p.col() == 0) {
            whiteQueenSide = false;
        } else if (p.row() == 7 && p.col() == 7) {
            whiteKingSide = false;
        } else if (p.row() == 0 && p.col() == 0) {
            blackQueenSide = false;
        } else if (p.row() == 0 && p.col() == 7) {
            blackKingSide = false;
        }
    }
}
