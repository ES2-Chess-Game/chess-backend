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
    private boolean whiteKingSideCastleAvailable = true;
    private boolean whiteQueenSideCastleAvailable = true;
    private boolean blackKingSideCastleAvailable = true;
    private boolean blackQueenSideCastleAvailable = true;
    private Position enPassantTarget;

    public Board() {
        setupInitialPosition();
    }

    public Board(Board other) {
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(other.squares[row], 0, squares[row], 0, SIZE);
        }
        whiteKingSideCastleAvailable = other.whiteKingSideCastleAvailable;
        whiteQueenSideCastleAvailable = other.whiteQueenSideCastleAvailable;
        blackKingSideCastleAvailable = other.blackKingSideCastleAvailable;
        blackQueenSideCastleAvailable = other.blackQueenSideCastleAvailable;
        enPassantTarget = other.enPassantTarget;
    }

    public Board copy() {
        return new Board(this);
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

    public boolean canCastle(Color color, boolean kingSide) {
        if (color == Color.BRANCA) {
            return kingSide ? whiteKingSideCastleAvailable : whiteQueenSideCastleAvailable;
        }
        return kingSide ? blackKingSideCastleAvailable : blackQueenSideCastleAvailable;
    }

    public void revokeCastlingRights(Color color) {
        revokeCastlingRight(color, true);
        revokeCastlingRight(color, false);
    }

    public void revokeCastlingRight(Color color, boolean kingSide) {
        if (color == Color.BRANCA) {
            if (kingSide) {
                whiteKingSideCastleAvailable = false;
            } else {
                whiteQueenSideCastleAvailable = false;
            }
        } else if (kingSide) {
            blackKingSideCastleAvailable = false;
        } else {
            blackQueenSideCastleAvailable = false;
        }
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(Position enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    public static boolean isInside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public static boolean isInside(Position p) {
        return isInside(p.row(), p.col());
    }
}
