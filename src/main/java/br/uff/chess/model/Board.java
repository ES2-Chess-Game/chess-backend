package br.uff.chess.model;

import static br.uff.chess.model.PieceType.BISPO;
import static br.uff.chess.model.PieceType.CAVALO;
import static br.uff.chess.model.PieceType.PEAO;
import static br.uff.chess.model.PieceType.REI;
import static br.uff.chess.model.PieceType.RAINHA;
import static br.uff.chess.model.PieceType.TORRE;

public class Board {

    public static final int SIZE = 8;

    private final Piece[][] squares = new Piece[SIZE][SIZE];

    public Board() {
        setupInitialPosition();
    }

    private void setupInitialPosition() {
        PieceType[] backRow = {TORRE, CAVALO, BISPO, RAINHA, REI, BISPO, CAVALO, TORRE};
        for (int c = 0; c < SIZE; c++) {
            squares[0][c] = new Piece(backRow[c], Color.PRETA);
            squares[1][c] = new Piece(PEAO, Color.PRETA);
            squares[6][c] = new Piece(PEAO, Color.BRANCA);
            squares[7][c] = new Piece(backRow[c], Color.BRANCA);
        }
    }

    public Piece get(int row, int col) {
        return squares[row][col];
    }

    public Piece get(Position p) {
        return squares[p.row()][p.col()];
    }

    public void set(int row, int col, Piece p) {
        squares[row][col] = p;
    }

    public void set(Position p, Piece piece) {
        squares[p.row()][p.col()] = piece;
    }

    public static boolean isInside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public static boolean isInside(Position p) {
        return isInside(p.row(), p.col());
    }
}
