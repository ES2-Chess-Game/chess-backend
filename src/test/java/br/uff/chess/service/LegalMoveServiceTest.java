package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

class LegalMoveServiceTest {

    private final LegalMoveService legalMoveService = new LegalMoveService();

    @Test
    void rejectsMoveThatLeavesOwnKingInCheck() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(6, 4, new Piece(PieceType.TORRE, Color.BRANCA));
        board.set(0, 4, new Piece(PieceType.TORRE, Color.PRETA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));

        assertFalse(legalMoveService.isLegalMove(board, new Position(6, 4), new Position(6, 3), Color.BRANCA));
    }

    @Test
    void permitsMoveThatGetsKingOutOfCheck() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 4, new Piece(PieceType.TORRE, Color.PRETA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));

        assertTrue(legalMoveService.isLegalMove(board, new Position(7, 4), new Position(7, 5), Color.BRANCA));
    }

    @Test
    void rejectsMoveIntoSquareAttackedByEnemyKing() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(5, 5, new Piece(PieceType.REI, Color.PRETA));

        assertFalse(legalMoveService.isLegalMove(board, new Position(7, 4), new Position(6, 5), Color.BRANCA));
    }

    @Test
    void doesNotAllowCapturingEnemyKing() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));
        board.set(1, 0, new Piece(PieceType.TORRE, Color.BRANCA));

        assertFalse(legalMoveService.isLegalMove(board, new Position(1, 0), new Position(0, 0), Color.BRANCA));
    }

    private Board emptyBoard() {
        Board board = new Board();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board.set(row, col, null);
            }
        }
        return board;
    }
}