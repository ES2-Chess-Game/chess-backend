package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

    @Test
    void returnsPossibleMovesForSelectedPiece() {
        LegalMoveService legalMoveService = new LegalMoveService();
        GameService gameService = new GameService(legalMoveService, new MinimaxAI(legalMoveService));
        Game game = gameService.createGame();

        // White pawn at (6, 4) [e2] can move to (5, 4) and (4, 4)
        List<Position> pawnMoves = gameService.getPossibleMoves(game.getId(), new Position(6, 4));
        assertEquals(2, pawnMoves.size());
        assertTrue(pawnMoves.contains(new Position(5, 4)));
        assertTrue(pawnMoves.contains(new Position(4, 4)));

        // White knight at (7, 1) [b1] can move to (5, 0) [a3] and (5, 2) [c3]
        List<Position> knightMoves = gameService.getPossibleMoves(game.getId(), new Position(7, 1));
        assertEquals(2, knightMoves.size());
        assertTrue(knightMoves.contains(new Position(5, 0)));
        assertTrue(knightMoves.contains(new Position(5, 2)));

        // Opponent piece (black pawn at 1, 4) cannot move when it's white's turn
        List<Position> blackPieceMoves = gameService.getPossibleMoves(game.getId(), new Position(1, 4));
        assertTrue(blackPieceMoves.isEmpty());

        // Empty square returns empty list
        List<Position> emptySquareMoves = gameService.getPossibleMoves(game.getId(), new Position(4, 4));
        assertTrue(emptySquareMoves.isEmpty());
    }
}
