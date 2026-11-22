package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.Position;

class GameServiceTest {

    @Test
    void marksCheckAndAllowsTheCheckedPlayerToRespond() {
        LegalMoveService legalMoveService = new LegalMoveService();
        GameService gameService = new GameService(legalMoveService, new MinimaxAI(legalMoveService));
        Game game = gameService.createGame();

        gameService.move(game.getId(), new Position(6, 4), new Position(4, 4));
        gameService.move(game.getId(), new Position(1, 4), new Position(3, 4));
        gameService.move(game.getId(), new Position(7, 3), new Position(3, 7));
        gameService.move(game.getId(), new Position(0, 1), new Position(2, 2));
        gameService.move(game.getId(), new Position(3, 7), new Position(3, 4));

        assertEquals(GameStatus.XEQUE, game.getStatus());
        assertEquals(Color.PRETA, game.getTurnoAtual());

        gameService.move(game.getId(), new Position(0, 3), new Position(1, 4));

        assertEquals(GameStatus.EM_ANDAMENTO, game.getStatus());
        assertEquals(Color.BRANCA, game.getTurnoAtual());
    }
}
