package br.uff.chess.service.rules;

import org.springframework.stereotype.Component;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

@Component
public class DefaultCheckDetector implements CheckDetector {

    @Override
    public boolean isKingInCheck(Board board, Color color) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece != null && piece.type() == PieceType.REI && piece.color() == color) {
                    Color opponent = color == Color.BRANCA ? Color.PRETA : Color.BRANCA;
                    return isSquareAttacked(board, new Position(row, col), opponent);
                }
            }
        }
        throw new IllegalArgumentException("Rei ausente no tabuleiro: " + color);
    }

    /**
     * Ataques independem da ocupação do destino e da legalidade do lance:
     * peões ameaçam diagonais vazias e peças cravadas continuam ameaçando casas.
     */
    public boolean isSquareAttacked(Board board, Position target, Color attackingColor) {
        if (!Board.isInside(target)) {
            return false;
        }
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece != null && piece.color() == attackingColor
                        && attacks(board, new Position(row, col), target, piece)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attacks(Board board, Position from, Position to, Piece piece) {
        int dr = to.row() - from.row();
        int dc = to.col() - from.col();
        if (dr == 0 && dc == 0) {
            return false;
        }
        boolean diagonal = Math.abs(dr) == Math.abs(dc);
        boolean straight = dr == 0 || dc == 0;
        return switch (piece.type()) {
            case PEAO -> dr == (piece.color() == Color.BRANCA ? -1 : 1) && Math.abs(dc) == 1;
            case CAVALO -> Math.abs(dr) * Math.abs(dc) == 2;
            case REI -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
            case BISPO -> diagonal && pathIsClear(board, from, to);
            case TORRE -> straight && pathIsClear(board, from, to);
            case RAINHA -> (diagonal || straight) && pathIsClear(board, from, to);
        };
    }

    private boolean pathIsClear(Board board, Position from, Position to) {
        int dr = Integer.signum(to.row() - from.row());
        int dc = Integer.signum(to.col() - from.col());
        int row = from.row() + dr;
        int col = from.col() + dc;
        while (row != to.row() || col != to.col()) {
            if (board.get(row, col) != null) {
                return false;
            }
            row += dr;
            col += dc;
        }
        return true;
    }
}
