package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

class GameServiceAiTest {

    @Test
    void aiAppliesLegalMoveAndUpdatesTurnThroughGameService() {
        DefaultCheckDetector detector = new DefaultCheckDetector();
        GameService service = new GameService(detector, new DefaultLegalMoveGenerator(detector));
        Game game = service.createGame();
        Board initialBoard = game.getBoard().copy();

        Game updatedGame = service.playAiMove(game.getId());

        assertEquals(Color.PRETA, updatedGame.getTurnoAtual());
        assertEquals(GameStatus.EM_ANDAMENTO, updatedGame.getStatus());
        assertTrue(boardChanged(initialBoard, updatedGame.getBoard()));
    }

    private boolean boardChanged(Board first, Board second) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (!java.util.Objects.equals(first.get(row, col), second.get(row, col))) {
                    return true;
                }
            }
        }
        return false;
    }
}
