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

class BishopValidatorTest {

    private final BishopValidator validator = new BishopValidator();
    private final Board board = emptyBoard();

    private boolean valid(String from, String to) {
        return validator.isValid(board, pos(from), pos(to), BRANCA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a1", "h8", "a7", "g1", "e5", "c3"})
    void andaNaDiagonalEmQualquerDirecao(String destino) {
        put(board, "d4", PieceType.BISPO, BRANCA);
        assertTrue(valid("d4", destino));
    }

    @ParameterizedTest
    @ValueSource(strings = {"d8", "h4", "e6", "d4"})
    void naoAndaEmLinhaRetaForaDaDiagonalNemFicaParado(String destino) {
        put(board, "d4", PieceType.BISPO, BRANCA);
        assertFalse(valid("d4", destino));
    }

    @Test
    void naoPulaPecas() {
        put(board, "d4", PieceType.BISPO, BRANCA);
        put(board, "f6", PieceType.PEAO, PRETA);

        assertTrue(valid("d4", "f6"));
        assertFalse(valid("d4", "g7"));
    }

    @Test
    void capturaAdversariaMasNaoPecaPropria() {
        put(board, "d4", PieceType.BISPO, BRANCA);
        put(board, "g7", PieceType.TORRE, PRETA);
        put(board, "b2", PieceType.TORRE, BRANCA);

        assertTrue(valid("d4", "g7"));
        assertFalse(valid("d4", "b2"));
    }
}
