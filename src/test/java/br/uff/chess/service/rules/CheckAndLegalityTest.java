package br.uff.chess.service.rules;

import static org.junit.jupiter.api.Assertions.*;
import static br.uff.chess.model.Color.*;
import static br.uff.chess.model.PieceType.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import br.uff.chess.model.*;

class CheckAndLegalityTest {
    private final DefaultCheckDetector detector = new DefaultCheckDetector();
    private final DefaultLegalMoveGenerator moves = new DefaultLegalMoveGenerator(detector);

    @ParameterizedTest
    @EnumSource(Color.class)
    void peaoAmeacaDiagonaisVaziasMasNaoFrente(Color color) {
        Board board = emptyBoard();
        board.set(3, 3, new Piece(PEAO, color));
        int nextRow = color == BRANCA ? 2 : 4;
        assertTrue(attacked(board, nextRow, 2, color));
        assertTrue(attacked(board, nextRow, 4, color));
        assertFalse(attacked(board, nextRow, 3, color));
        assertFalse(attacked(board, 6 - nextRow, 2, color));
    }

    @ParameterizedTest
    @EnumSource(value = PieceType.class, names = {"TORRE", "BISPO", "RAINHA"})
    void pecasDeLinhaRespeitamBloqueioEAmeacamCasaOcupadaPorAliada(PieceType type) {
        Board board = emptyBoard();
        board.set(0, 0, new Piece(type, PRETA));
        int col = type == BISPO ? 3 : 0;
        assertTrue(attacked(board, 3, col, PRETA));
        board.set(3, col, new Piece(CAVALO, PRETA));
        assertTrue(attacked(board, 3, col, PRETA));
        assertFalse(attacked(board, 4, type == BISPO ? 4 : 0, PRETA));
        board.set(3, col, new Piece(CAVALO, BRANCA));
        assertFalse(attacked(board, 4, type == BISPO ? 4 : 0, PRETA));
        assertFalse(attacked(board, 0, 0, PRETA));
    }

    @Test
    void rainhaAmeacaDiagonalECavaloSaltaPecas() {
        Board board = emptyBoard();
        board.set(0, 0, new Piece(RAINHA, BRANCA));
        assertTrue(attacked(board, 7, 7, BRANCA));
        assertFalse(attacked(board, 2, 3, BRANCA));
        board.set(0, 0, new Piece(CAVALO, BRANCA));
        board.set(1, 0, new Piece(PEAO, PRETA));
        board.set(1, 1, new Piece(PEAO, PRETA));
        assertTrue(attacked(board, 2, 1, BRANCA));
        assertTrue(attacked(board, 1, 2, BRANCA));
        assertFalse(attacked(board, 2, 2, BRANCA));
    }

    @Test
    void pecaCravadaAindaAmeacaCasas() {
        Board board = emptyBoard();
        board.set(0, 4, new Piece(REI, PRETA));
        board.set(1, 4, new Piece(CAVALO, PRETA));
        board.set(7, 4, new Piece(TORRE, BRANCA));
        board.set(4, 3, new Piece(REI, BRANCA));
        assertTrue(attacked(board, 3, 3, PRETA));
        assertFalse(legal(board, 4, 3, 3, 3, BRANCA));
    }

    @Test
    void reisNaoPodemFicarAdjacentesNemSerCapturados() {
        Board board = emptyBoard();
        board.set(0, 4, new Piece(REI, PRETA));
        board.set(2, 4, new Piece(REI, BRANCA));
        assertFalse(legal(board, 2, 4, 1, 4, BRANCA));
        board.set(0, 0, new Piece(TORRE, BRANCA));
        assertFalse(legal(board, 0, 0, 0, 4, BRANCA));
    }

    @Test
    void reiNaoPodeEntrarEmDiagonalDePeaoOuCapturarPecaProtegida() {
        Board board = emptyBoard();
        board.set(0, 7, new Piece(REI, PRETA));
        board.set(4, 4, new Piece(REI, BRANCA));
        board.set(2, 2, new Piece(PEAO, PRETA));
        assertFalse(legal(board, 4, 4, 3, 3, BRANCA));
        board.set(3, 3, new Piece(CAVALO, PRETA));
        assertFalse(legal(board, 4, 4, 3, 3, BRANCA));
        board.set(2, 2, null);
        assertTrue(legal(board, 4, 4, 3, 3, BRANCA));
    }

    @Test
    void xequeExigeFugaBloqueioOuCapturaDoAtacante() {
        Board board = emptyBoard();
        board.set(0, 0, new Piece(REI, PRETA));
        board.set(7, 4, new Piece(REI, BRANCA));
        board.set(0, 4, new Piece(TORRE, PRETA));
        board.set(6, 0, new Piece(TORRE, BRANCA));
        assertTrue(detector.isKingInCheck(board, BRANCA));
        assertFalse(legal(board, 6, 0, 5, 0, BRANCA));
        assertTrue(legal(board, 6, 0, 6, 4, BRANCA));
        assertTrue(legal(board, 7, 4, 7, 3, BRANCA));
        board.set(0, 7, new Piece(TORRE, BRANCA));
        assertTrue(legal(board, 0, 7, 0, 4, BRANCA));
    }

    @Test
    void reiNaoPodeRecuarNaLinhaQueEleMesmoBloqueava() {
        Board board = emptyBoard();
        board.set(0, 0, new Piece(REI, PRETA));
        board.set(0, 4, new Piece(TORRE, PRETA));
        board.set(6, 4, new Piece(REI, BRANCA));
        assertFalse(attacked(board, 7, 4, PRETA));
        assertFalse(legal(board, 6, 4, 7, 4, BRANCA));
    }

    @Test
    void posicaoInicialTemVinteLancesLegaisESemXequeParaCadaCor() {
        Board board = new Board();
        for (Color color : Color.values()) {
            assertFalse(detector.isKingInCheck(board, color));
            assertTrue(moves.hasLegalMove(board, color));
            int count = 0;
            for (int from = 0; from < 64; from++) {
                for (int to = 0; to < 64; to++) {
                    if (legal(board, from / 8, from % 8, to / 8, to % 8, color)) {
                        count++;
                    }
                }
            }
            assertEquals(20, count);
        }
    }

    @Test
    void recusaOrigemVaziaCorErradaDestinoAliadoECoordenadasInvalidas() {
        Board board = new Board();
        assertFalse(legal(board, 4, 4, 3, 4, BRANCA));
        assertFalse(legal(board, 1, 0, 2, 0, BRANCA));
        assertFalse(legal(board, 7, 0, 6, 0, BRANCA));
        assertFalse(legal(board, 6, 0, 6, 0, BRANCA));
        assertFalse(legal(board, -1, 0, 2, 0, PRETA));
        assertFalse(legal(board, 1, 0, 8, 0, PRETA));
    }

    private boolean attacked(Board board, int row, int col, Color color) {
        return detector.isSquareAttacked(board, new Position(row, col), color);
    }

    private boolean legal(Board board, int row, int col, int toRow, int toCol, Color color) {
        return moves.isLegalMove(board, new Position(row, col), new Position(toRow, toCol), color);
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
