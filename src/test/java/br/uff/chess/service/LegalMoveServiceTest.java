package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

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

    @Test
    void allowsKingsideCastlingAndMovesTheRook() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(7, 7, new Piece(PieceType.TORRE, Color.BRANCA));
        board.set(0, 4, new Piece(PieceType.REI, Color.PRETA));

        assertTrue(legalMoveService.isLegalMove(board, new Position(7, 4), new Position(7, 6), Color.BRANCA));

        LegalMoveService.applyMove(board, new Position(7, 4), new Position(7, 6));

        assertEquals(new Piece(PieceType.REI, Color.BRANCA), board.get(7, 6));
        assertEquals(new Piece(PieceType.TORRE, Color.BRANCA), board.get(7, 5));
        assertFalse(legalMoveService.isLegalMove(board, new Position(7, 6), new Position(7, 4), Color.BRANCA));
    }

    @Test
    void rejectsCastlingThroughAnAttackedSquare() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(7, 7, new Piece(PieceType.TORRE, Color.BRANCA));
        board.set(0, 5, new Piece(PieceType.TORRE, Color.PRETA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));

        assertFalse(legalMoveService.isLegalMove(board, new Position(7, 4), new Position(7, 6), Color.BRANCA));
    }

    @Test
    void doesNotRestoreCastlingRightsAfterKingReturnsHome() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(7, 7, new Piece(PieceType.TORRE, Color.BRANCA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));
        LegalMoveService.applyMove(board, new Position(7, 4), new Position(6, 4));
        LegalMoveService.applyMove(board, new Position(6, 4), new Position(7, 4));

        assertFalse(legalMoveService.isLegalMove(board, new Position(7, 4), new Position(7, 6), Color.BRANCA));
    }

    @Test
    void allowsEnPassantOnlyImmediatelyAfterTheDoublePawnMove() {
        Board board = emptyBoard();
        board.set(7, 7, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));
        board.set(3, 4, new Piece(PieceType.PEAO, Color.BRANCA));
        board.set(1, 3, new Piece(PieceType.PEAO, Color.PRETA));
        LegalMoveService.applyMove(board, new Position(1, 3), new Position(3, 3));

        assertTrue(legalMoveService.isLegalMove(board.copy(), new Position(3, 4), new Position(2, 3), Color.BRANCA));
        LegalMoveService.applyMove(board, new Position(3, 4), new Position(2, 3));
        assertEquals(new Piece(PieceType.PEAO, Color.BRANCA), board.get(2, 3));
        assertNull(board.get(3, 3));
    }

    @Test
    void expiresEnPassantAfterAnInterveningMove() {
        Board board = emptyBoard();
        board.set(7, 7, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));
        board.set(3, 4, new Piece(PieceType.PEAO, Color.BRANCA));
        board.set(1, 3, new Piece(PieceType.PEAO, Color.PRETA));
        LegalMoveService.applyMove(board, new Position(1, 3), new Position(3, 3));
        LegalMoveService.applyMove(board, new Position(7, 7), new Position(6, 7));
        LegalMoveService.applyMove(board, new Position(0, 0), new Position(0, 1));

        assertFalse(legalMoveService.isLegalMove(board, new Position(3, 4), new Position(2, 3), Color.BRANCA));
    }

    @Test
    void rejectsEnPassantWhenItWouldExposeTheKing() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));
        board.set(0, 4, new Piece(PieceType.TORRE, Color.PRETA));
        board.set(3, 4, new Piece(PieceType.PEAO, Color.BRANCA));
        board.set(1, 3, new Piece(PieceType.PEAO, Color.PRETA));
        LegalMoveService.applyMove(board, new Position(1, 3), new Position(3, 3));

        assertFalse(legalMoveService.isLegalMove(board, new Position(3, 4), new Position(2, 3), Color.BRANCA));
    }

    @Test
    void promotesPawnToSelectedPieceOrQueenByDefault() {
        Board board = emptyBoard();
        board.set(7, 7, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 7, new Piece(PieceType.REI, Color.PRETA));
        board.set(1, 0, new Piece(PieceType.PEAO, Color.BRANCA));
        Position from = new Position(1, 0);
        Position to = new Position(0, 0);

        assertTrue(legalMoveService.isLegalMove(board, from, to, Color.BRANCA));
        LegalMoveService.applyMove(board, from, to, PieceType.CAVALO);
        assertEquals(new Piece(PieceType.CAVALO, Color.BRANCA), board.get(to));

        board.set(1, 1, new Piece(PieceType.PEAO, Color.BRANCA));
        LegalMoveService.applyMove(board, new Position(1, 1), new Position(0, 1));
        assertEquals(new Piece(PieceType.RAINHA, Color.BRANCA), board.get(0, 1));
    }

    @Test
    void generatesAllFourPromotionChoicesForTheAi() {
        Board board = emptyBoard();
        board.set(7, 7, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 7, new Piece(PieceType.REI, Color.PRETA));
        board.set(1, 0, new Piece(PieceType.PEAO, Color.BRANCA));

        Set<PieceType> promotions = legalMoveService.getLegalMoves(board, Color.BRANCA).stream()
                .filter(move -> move.from().equals(new Position(1, 0)) &&
                        move.to().equals(new Position(0, 0)))
                .map(LegalMove::promotion)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(PieceType.RAINHA, PieceType.TORRE, PieceType.BISPO, PieceType.CAVALO), promotions);
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