package br.uff.chess;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

/** Helpers de teste: tabuleiro vazio e notação algébrica (e2 -> linha 6, coluna 4). */
public final class TestBoards {

    private TestBoards() {
    }

    public static Position pos(String square) {
        return new Position(8 - (square.charAt(1) - '0'), square.charAt(0) - 'a');
    }

    public static Board emptyBoard() {
        Board board = new Board();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board.set(row, col, null);
            }
        }
        return board;
    }

    public static void put(Board board, String square, PieceType type, Color color) {
        board.set(pos(square), new Piece(type, color));
    }
}
