package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;

class MinimaxAITest {

    @Test
    void difficultiesUseIncreasingSearchDepth() {
        assertEquals(1, AiDifficulty.EASY.searchDepth());
        assertEquals(2, AiDifficulty.MEDIUM.searchDepth());
        assertEquals(3, AiDifficulty.ADVANCED.searchDepth());
    }

    @Test
    void choosesLegalMoveWithoutMutatingOriginalBoard() {
        Board board = new Board();
        LegalMoveService legalMoveService = new LegalMoveService();
        MinimaxAI ai = new MinimaxAI(legalMoveService);

        for (AiDifficulty difficulty : AiDifficulty.values()) {
            LegalMove move = ai.chooseMove(board, Color.BRANCA, difficulty).orElseThrow();

            assertTrue(legalMoveService.isLegalMove(board, move.from(), move.to(), Color.BRANCA));
            assertEquals(Color.BRANCA, board.get(move.from()).color());
            assertNull(board.get(move.to()));
        }
    }
}