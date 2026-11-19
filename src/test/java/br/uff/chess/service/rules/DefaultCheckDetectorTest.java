package br.uff.chess.service.rules;

import static br.uff.chess.TestBoards.emptyBoard;
import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.TestBoards.put;
import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

class DefaultCheckDetectorTest {

    private final DefaultCheckDetector detector = new DefaultCheckDetector();
    private final Board board = emptyBoard();

    @Test
    void detectaXequeDeCadaTipoDePeca() {
        put(board, "e1", PieceType.REI, BRANCA);
        put(board, "a8", PieceType.REI, PRETA);

        put(board, "e8", PieceType.TORRE, PRETA);
        assertTrue(detector.isKingInCheck(board, BRANCA));
        board.set(pos("e8"), null);

        put(board, "a5", PieceType.BISPO, PRETA);
        assertTrue(detector.isKingInCheck(board, BRANCA));
        board.set(pos("a5"), null);

        put(board, "f3", PieceType.CAVALO, PRETA);
        assertTrue(detector.isKingInCheck(board, BRANCA));
        board.set(pos("f3"), null);

        put(board, "d2", PieceType.PEAO, PRETA);
        assertTrue(detector.isKingInCheck(board, BRANCA));
        board.set(pos("d2"), null);

        assertFalse(detector.isKingInCheck(board, BRANCA));
    }

    @Test
    void pecaNoCaminhoBloqueiaOXeque() {
        put(board, "e1", PieceType.REI, BRANCA);
        put(board, "a8", PieceType.REI, PRETA);
        put(board, "e8", PieceType.RAINHA, PRETA);
        put(board, "e4", PieceType.CAVALO, BRANCA);

        assertFalse(detector.isKingInCheck(board, BRANCA));
    }

    @Test
    void reiAusenteLancaExcecao() {
        put(board, "a8", PieceType.REI, PRETA);
        assertThrows(IllegalArgumentException.class, () -> detector.isKingInCheck(board, BRANCA));
    }

    @Test
    void casaForaDoTabuleiroNuncaEstaAtacada() {
        put(board, "a1", PieceType.RAINHA, BRANCA);
        assertFalse(detector.isSquareAttacked(board, new Position(-1, 0), BRANCA));
        assertFalse(detector.isSquareAttacked(board, new Position(0, 8), BRANCA));
    }
}
