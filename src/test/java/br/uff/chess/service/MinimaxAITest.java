package br.uff.chess.service;

import static br.uff.chess.TestBoards.emptyBoard;
import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.TestBoards.put;
import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.rules.AttackedSquares;

class MinimaxAITest {

    private final LegalMoveService legalMoveService = new LegalMoveService();
    private final MinimaxAI ai = new MinimaxAI(legalMoveService);

    @Test
    void choosesLegalMoveWithoutMutatingOriginalBoard() {
        Board board = new Board();

        LegalMove move = ai.chooseMove(board, Color.BRANCA).orElseThrow();

        assertTrue(legalMoveService.isLegalMove(board, move.from(), move.to(), Color.BRANCA));
        assertEquals(Color.BRANCA, board.get(move.from()).color());
        assertNull(board.get(move.to()));
    }

    @Test
    void encontraMateEmUm() {
        Board board = emptyBoard();
        put(board, "g1", PieceType.REI, BRANCA);
        put(board, "a1", PieceType.TORRE, BRANCA);
        put(board, "f2", PieceType.PEAO, BRANCA);
        put(board, "g2", PieceType.PEAO, BRANCA);
        put(board, "h2", PieceType.PEAO, BRANCA);
        put(board, "g8", PieceType.REI, PRETA);
        put(board, "f7", PieceType.PEAO, PRETA);
        put(board, "g7", PieceType.PEAO, PRETA);
        put(board, "h7", PieceType.PEAO, PRETA);

        LegalMove move = ai.chooseMove(board, BRANCA).orElseThrow();

        assertEquals(new LegalMove(pos("a1"), pos("a8")), move);
    }

    @Test
    void pretasTambemEncontramMateEmUm() {
        Board board = emptyBoard();
        put(board, "g8", PieceType.REI, PRETA);
        put(board, "a8", PieceType.TORRE, PRETA);
        put(board, "f7", PieceType.PEAO, PRETA);
        put(board, "g7", PieceType.PEAO, PRETA);
        put(board, "h7", PieceType.PEAO, PRETA);
        put(board, "g1", PieceType.REI, BRANCA);
        put(board, "f2", PieceType.PEAO, BRANCA);
        put(board, "g2", PieceType.PEAO, BRANCA);
        put(board, "h2", PieceType.PEAO, BRANCA);

        LegalMove move = ai.chooseMove(board, PRETA).orElseThrow();

        assertEquals(new LegalMove(pos("a8"), pos("a1")), move);
    }

    @Test
    void capturaRainhaIndefesa() {
        Board board = emptyBoard();
        put(board, "e1", PieceType.REI, BRANCA);
        put(board, "c3", PieceType.CAVALO, BRANCA);
        put(board, "h8", PieceType.REI, PRETA);
        put(board, "d5", PieceType.RAINHA, PRETA);

        LegalMove move = ai.chooseMove(board, BRANCA).orElseThrow();

        assertEquals(new LegalMove(pos("c3"), pos("d5")), move);
    }

    @Test
    void naoDeixaARainhaSerCapturadaDeGraca() {
        Board board = emptyBoard();
        put(board, "e1", PieceType.REI, BRANCA);
        put(board, "d4", PieceType.RAINHA, BRANCA);
        put(board, "h8", PieceType.REI, PRETA);
        put(board, "c5", PieceType.PEAO, PRETA); // ataca d4
        put(board, "b6", PieceType.PEAO, PRETA); // protege c5

        LegalMove move = ai.chooseMove(board, BRANCA).orElseThrow();

        Board next = board.copy();
        LegalMoveService.applyMove(next, move.from(), move.to());
        Position rainha = findQueen(next, BRANCA);
        assertFalse(AttackedSquares.isAttacked(next, rainha, PRETA), () -> "rainha ficou atacada em " + rainha);
    }

    @Test
    void semLancesLegaisNaoDevolveJogada() {
        Board board = emptyBoard();
        put(board, "h8", PieceType.REI, PRETA);
        put(board, "g6", PieceType.RAINHA, BRANCA);
        put(board, "a1", PieceType.REI, BRANCA);

        assertTrue(ai.chooseMove(board, PRETA).isEmpty());
    }

    private Position findQueen(Board board, Color color) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (new Piece(PieceType.RAINHA, color).equals(board.get(row, col))) {
                    return new Position(row, col);
                }
            }
        }
        throw new AssertionError("rainha " + color + " sumiu do tabuleiro");
    }
}
