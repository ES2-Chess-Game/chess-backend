package br.uff.chess.service.validator;

import static br.uff.chess.TestBoards.emptyBoard;
import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.TestBoards.put;
import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.uff.chess.model.Board;
import br.uff.chess.model.PieceType;

class KnightValidatorTest {

    private final KnightValidator validator = new KnightValidator();
    private final Board board = emptyBoard();

    private boolean valid(String from, String to) {
        return validator.isValid(board, pos(from), pos(to), BRANCA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"c6", "e6", "f5", "f3", "e2", "c2", "b3", "b5"})
    void andaEmLNasOitoDirecoes(String destino) {
        put(board, "d4", PieceType.CAVALO, BRANCA);
        assertTrue(valid("d4", destino));
    }

    @ParameterizedTest
    @ValueSource(strings = {"d5", "e5", "d6", "f6", "d4", "g4"})
    void recusaQualquerCasaForaDoL(String destino) {
        put(board, "d4", PieceType.CAVALO, BRANCA);
        assertFalse(valid("d4", destino));
    }

    @Test
    void saltaPorCimaDasPecas() {
        Board inicial = new Board();
        assertTrue(validator.isValid(inicial, pos("g1"), pos("f3"), BRANCA));
    }

    @Test
    void capturaAdversariaMasNaoPecaPropria() {
        put(board, "d4", PieceType.CAVALO, BRANCA);
        put(board, "e6", PieceType.PEAO, PRETA);
        put(board, "c6", PieceType.PEAO, BRANCA);

        assertTrue(valid("d4", "e6"));
        assertFalse(valid("d4", "c6"));
    }
}
