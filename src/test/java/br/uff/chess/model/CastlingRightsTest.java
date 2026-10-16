package br.uff.chess.model;

import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CastlingRightsTest {

    private final CastlingRights rights = new CastlingRights();

    @Test
    void revokeAllRemoveApenasOsDireitosDaCorInformada() {
        rights.revokeAll(BRANCA);

        assertFalse(rights.canKingSide(BRANCA));
        assertFalse(rights.canQueenSide(BRANCA));
        assertTrue(rights.canKingSide(PRETA));
        assertTrue(rights.canQueenSide(PRETA));
    }

    @Test
    void revokeAllDasPretasNaoAfetaAsBrancas() {
        rights.revokeAll(PRETA);

        assertFalse(rights.canKingSide(PRETA));
        assertFalse(rights.canQueenSide(PRETA));
        assertTrue(rights.canKingSide(BRANCA));
        assertTrue(rights.canQueenSide(BRANCA));
    }

    @Test
    void cantoA1RevogaSoORoqueGrandeDasBrancas() {
        rights.revokeForSquare(new Position(7, 0));

        assertFalse(rights.canQueenSide(BRANCA));
        assertTrue(rights.canKingSide(BRANCA));
        assertTrue(rights.canKingSide(PRETA));
        assertTrue(rights.canQueenSide(PRETA));
    }

    @Test
    void cantoH1RevogaSoORoquePequenoDasBrancas() {
        rights.revokeForSquare(new Position(7, 7));

        assertFalse(rights.canKingSide(BRANCA));
        assertTrue(rights.canQueenSide(BRANCA));
        assertTrue(rights.canKingSide(PRETA));
        assertTrue(rights.canQueenSide(PRETA));
    }

    @Test
    void cantoA8RevogaSoORoqueGrandeDasPretas() {
        rights.revokeForSquare(new Position(0, 0));

        assertFalse(rights.canQueenSide(PRETA));
        assertTrue(rights.canKingSide(PRETA));
        assertTrue(rights.canKingSide(BRANCA));
        assertTrue(rights.canQueenSide(BRANCA));
    }

    @Test
    void cantoH8RevogaSoORoquePequenoDasPretas() {
        rights.revokeForSquare(new Position(0, 7));

        assertFalse(rights.canKingSide(PRETA));
        assertTrue(rights.canQueenSide(PRETA));
        assertTrue(rights.canKingSide(BRANCA));
        assertTrue(rights.canQueenSide(BRANCA));
    }

    @Test
    void casaQueNaoEhCantoNaoRevogaNada() {
        rights.revokeForSquare(new Position(7, 4));
        rights.revokeForSquare(new Position(0, 4));
        rights.revokeForSquare(new Position(3, 3));

        for (Color color : Color.values()) {
            assertTrue(rights.canKingSide(color));
            assertTrue(rights.canQueenSide(color));
        }
    }
}
