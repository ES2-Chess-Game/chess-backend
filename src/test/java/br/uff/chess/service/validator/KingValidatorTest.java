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

class KingValidatorTest {

    private final KingValidator validator = new KingValidator();
    private final Board board = emptyBoard();

    private boolean valid(String from, String to) {
        return validator.isValid(board, pos(from), pos(to), BRANCA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"c5", "d5", "e5", "c4", "e4", "c3", "d3", "e3"})
    void andaUmaCasaEmQualquerDirecao(String destino) {
        put(board, "d4", PieceType.REI, BRANCA);
        assertTrue(valid("d4", destino));
    }

    @ParameterizedTest
    @ValueSource(strings = {"d6", "f4", "b2", "e6", "d4"})
    void naoAndaMaisDeUmaCasaNemFicaParado(String destino) {
        put(board, "d4", PieceType.REI, BRANCA);
        assertFalse(valid("d4", destino));
    }

    @Test
    void capturaAdversariaMasNaoPecaPropria() {
        put(board, "d4", PieceType.REI, BRANCA);
        put(board, "d5", PieceType.PEAO, PRETA);
        put(board, "e5", PieceType.PEAO, BRANCA);

        assertTrue(valid("d4", "d5"));
        assertFalse(valid("d4", "e5"));
    }
}
