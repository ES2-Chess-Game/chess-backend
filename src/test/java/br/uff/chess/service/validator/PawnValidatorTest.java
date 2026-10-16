package br.uff.chess.service.validator;

import static br.uff.chess.TestBoards.emptyBoard;
import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.TestBoards.put;
import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.PieceType;

class PawnValidatorTest {

    private final PawnValidator validator = new PawnValidator();
    private final Board board = emptyBoard();

    private boolean valid(String from, String to, Color color) {
        return validator.isValid(board, pos(from), pos(to), color);
    }

    @Test
    void brancoAvancaUmaCasaParaCima() {
        put(board, "e3", PieceType.PEAO, BRANCA);
        assertTrue(valid("e3", "e4", BRANCA));
    }

    @Test
    void pretoAvancaUmaCasaParaBaixo() {
        put(board, "e6", PieceType.PEAO, PRETA);
        assertTrue(valid("e6", "e5", PRETA));
    }

    @Test
    void avancaDuasCasasSoNaFileiraInicial() {
        put(board, "e2", PieceType.PEAO, BRANCA);
        put(board, "d7", PieceType.PEAO, PRETA);
        put(board, "a3", PieceType.PEAO, BRANCA);

        assertTrue(valid("e2", "e4", BRANCA));
        assertTrue(valid("d7", "d5", PRETA));
        assertFalse(valid("a3", "a5", BRANCA));
    }

    @Test
    void naoAvancaTresCasas() {
        put(board, "e2", PieceType.PEAO, BRANCA);
        assertFalse(valid("e2", "e5", BRANCA));
    }

    @Test
    void avancoDuploBloqueadoPorPecaNoMeioOuNoDestino() {
        put(board, "e2", PieceType.PEAO, BRANCA);
        put(board, "e3", PieceType.CAVALO, PRETA);
        assertFalse(valid("e2", "e4", BRANCA));

        board.set(pos("e3"), null);
        put(board, "e4", PieceType.CAVALO, PRETA);
        assertFalse(valid("e2", "e4", BRANCA));
    }

    @Test
    void naoCapturaAndandoReto() {
        put(board, "e4", PieceType.PEAO, BRANCA);
        put(board, "e5", PieceType.PEAO, PRETA);
        assertFalse(valid("e4", "e5", BRANCA));
    }

    @Test
    void capturaNaDiagonalParaFrente() {
        put(board, "e4", PieceType.PEAO, BRANCA);
        put(board, "d5", PieceType.PEAO, PRETA);
        put(board, "f5", PieceType.CAVALO, PRETA);

        assertTrue(valid("e4", "d5", BRANCA));
        assertTrue(valid("e4", "f5", BRANCA));
    }

    @Test
    void pretoCapturaNaDiagonalParaBaixo() {
        put(board, "e5", PieceType.PEAO, PRETA);
        put(board, "d4", PieceType.PEAO, BRANCA);
        assertTrue(valid("e5", "d4", PRETA));
    }

    @Test
    void naoAndaNaDiagonalSemCapturaNemCapturaPecaPropria() {
        put(board, "e4", PieceType.PEAO, BRANCA);
        put(board, "f5", PieceType.CAVALO, BRANCA);

        assertFalse(valid("e4", "d5", BRANCA));
        assertFalse(valid("e4", "f5", BRANCA));
    }

    @Test
    void naoAndaParaTrasNemParaOLado() {
        put(board, "e4", PieceType.PEAO, BRANCA);
        put(board, "d3", PieceType.PEAO, PRETA);

        assertFalse(valid("e4", "e3", BRANCA));
        assertFalse(valid("e4", "d3", BRANCA));
        assertFalse(valid("e4", "f4", BRANCA));
    }

    @Test
    void naoCapturaDuasCasasNaDiagonal() {
        put(board, "e4", PieceType.PEAO, BRANCA);
        put(board, "c6", PieceType.PEAO, PRETA);
        assertFalse(valid("e4", "c6", BRANCA));
    }
}
