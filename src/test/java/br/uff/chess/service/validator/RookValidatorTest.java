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

class RookValidatorTest {

    private final RookValidator validator = new RookValidator();
    private final Board board = emptyBoard();

    private boolean valid(String from, String to) {
        return validator.isValid(board, pos(from), pos(to), BRANCA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"d8", "d1", "a4", "h4", "d5", "e4"})
    void andaEmLinhaRetaEmQualquerDirecao(String destino) {
        put(board, "d4", PieceType.TORRE, BRANCA);
        assertTrue(valid("d4", destino));
    }

    @ParameterizedTest
    @ValueSource(strings = {"e5", "a1", "e6", "c2"})
    void naoAndaNaDiagonalNemForaDeLinha(String destino) {
        put(board, "d4", PieceType.TORRE, BRANCA);
        assertFalse(valid("d4", destino));
    }

    @Test
    void naoPulaPecas() {
        put(board, "d4", PieceType.TORRE, BRANCA);
        put(board, "d6", PieceType.PEAO, PRETA);

        assertTrue(valid("d4", "d6"));
        assertFalse(valid("d4", "d7"));
    }

    @Test
    void capturaAdversariaMasNaoPecaPropria() {
        put(board, "d4", PieceType.TORRE, BRANCA);
        put(board, "h4", PieceType.CAVALO, PRETA);
        put(board, "a4", PieceType.CAVALO, BRANCA);

        assertTrue(valid("d4", "h4"));
        assertFalse(valid("d4", "a4"));
    }

    @Test
    void naoFicaNaMesmaCasa() {
        put(board, "d4", PieceType.TORRE, BRANCA);
        assertFalse(valid("d4", "d4"));
    }
}
