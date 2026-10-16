package br.uff.chess.model;

import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoardTest {

    @Test
    void posicaoInicialTemPecasNasFileirasCorretas() {
        Board board = new Board();
        PieceType[] fileiraDeTras = {PieceType.TORRE, PieceType.CAVALO, PieceType.BISPO, PieceType.RAINHA,
                PieceType.REI, PieceType.BISPO, PieceType.CAVALO, PieceType.TORRE};

        for (int col = 0; col < Board.SIZE; col++) {
            assertEquals(new Piece(fileiraDeTras[col], PRETA), board.get(0, col));
            assertEquals(new Piece(PieceType.PEAO, PRETA), board.get(1, col));
            assertEquals(new Piece(PieceType.PEAO, BRANCA), board.get(6, col));
            assertEquals(new Piece(fileiraDeTras[col], BRANCA), board.get(7, col));
            for (int row = 2; row <= 5; row++) {
                assertNull(board.get(row, col));
            }
        }
    }



    @Test
    void copiaEhIndependenteDoOriginal() {
        Board original = new Board();
        Board copia = original.copy();

        copia.set(pos("e2"), null);
        copia.set(pos("e4"), new Piece(PieceType.PEAO, BRANCA));

        assertEquals(new Piece(PieceType.PEAO, BRANCA), original.get(pos("e2")));
        assertNull(original.get(pos("e4")));
    }

    @Test
    void copiaPreservaTodasAsCasas() {
        Board original = new Board();
        original.set(pos("e4"), new Piece(PieceType.RAINHA, PRETA));
        Board copia = new Board(original);

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                assertEquals(original.get(row, col), copia.get(row, col));
            }
        }
    }

    @Test
    void isInsideAceitaApenasCasasDoTabuleiro() {
        assertTrue(Board.isInside(0, 0));
        assertTrue(Board.isInside(7, 7));
        assertTrue(Board.isInside(new Position(3, 4)));

        assertFalse(Board.isInside(-1, 0));
        assertFalse(Board.isInside(0, -1));
        assertFalse(Board.isInside(8, 0));
        assertFalse(Board.isInside(0, 8));
        assertFalse(Board.isInside(new Position(8, 8)));
    }
}
