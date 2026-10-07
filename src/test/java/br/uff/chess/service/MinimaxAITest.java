package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;

class MinimaxAITest {

    @Test
    void choosesLegalMoveWithoutMutatingOriginalBoard() {
        Board board = new Board();
        LegalMoveService legalMoveService = new LegalMoveService();
        MinimaxAI ai = new MinimaxAI(legalMoveService);

        LegalMove move = ai.chooseMove(board, Color.BRANCA).orElseThrow();

        assertTrue(legalMoveService.isLegalMove(board, move.from(), move.to(), Color.BRANCA));
        assertEquals(Color.BRANCA, board.get(move.from()).color());
        assertNull(board.get(move.to()));
    }
}