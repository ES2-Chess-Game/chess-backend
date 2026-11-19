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

class QueenValidatorTest {

    private final QueenValidator validator = new QueenValidator();
    private final Board board = emptyBoard();

    private boolean valid(String from, String to) {
        return validator.isValid(board, pos(from), pos(to), BRANCA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"d8", "d1", "a4", "h4", "a1", "h8", "a7", "g1"})
    void andaEmLinhaRetaENaDiagonal(String destino) {
        put(board, "d4", PieceType.RAINHA, BRANCA);
        assertTrue(valid("d4", destino));
    }

    @ParameterizedTest
    @ValueSource(strings = {"e6", "f5", "b3", "d4"})
    void naoAndaEmLNemForaDeLinhaNemFicaParada(String destino) {
        put(board, "d4", PieceType.RAINHA, BRANCA);
        assertFalse(valid("d4", destino));
    }

    @Test
    void naoPulaPecasEmNenhumaDirecao() {
        put(board, "d4", PieceType.RAINHA, BRANCA);
        put(board, "d6", PieceType.PEAO, PRETA);
        put(board, "f6", PieceType.PEAO, PRETA);

        assertFalse(valid("d4", "d7"));
        assertFalse(valid("d4", "g7"));
    }

    @Test
    void capturaAdversariaMasNaoPecaPropria() {
        put(board, "d4", PieceType.RAINHA, BRANCA);
        put(board, "d8", PieceType.TORRE, PRETA);
        put(board, "a1", PieceType.TORRE, BRANCA);

        assertTrue(valid("d4", "d8"));
        assertFalse(valid("d4", "a1"));
    }
}
