package br.uff.chess.service;

import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.TestBoards.put;
import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

class GameServiceTest {

    private final DefaultCheckDetector detector = new DefaultCheckDetector();
    private final GameService service = new GameService(detector, new DefaultLegalMoveGenerator(detector));

    @Test
    void criaPartidasComIdsDiferentesERecuperaPeloId() {
        Game primeira = service.createGame(7L);
        Game segunda = service.createGame();

        assertNotEquals(primeira.getId(), segunda.getId());
        assertSame(primeira, service.getGame(primeira.getId()));
        assertEquals(7L, primeira.getUserId());
        assertNull(segunda.getUserId());
        assertEquals(BRANCA, primeira.getTurnoAtual());
    }

    @Test
    void partidaInexistenteLancaExcecao() {
        assertThrows(GameNotFoundException.class, () -> service.getGame("nao-existe"));
        assertThrows(GameNotFoundException.class, () -> service.move("nao-existe", pos("e2"), pos("e4")));
    }

    @Test
    void recusaLancesInvalidosSemAlterarAPartida() {
        Game game = service.createGame();
        String id = game.getId();

        assertThrows(IllegalMoveException.class, () -> service.move(id, new Position(8, 4), pos("e4")));
        assertThrows(IllegalMoveException.class, () -> service.move(id, pos("e2"), new Position(-1, 4)));
        assertThrows(IllegalMoveException.class, () -> service.move(id, pos("e2"), pos("e2")));
        assertThrows(IllegalMoveException.class, () -> service.move(id, pos("e4"), pos("e5")));
        assertThrows(IllegalMoveException.class, () -> service.move(id, pos("e7"), pos("e5")));
        assertThrows(IllegalMoveException.class, () -> service.move(id, pos("e2"), pos("e5")));

        assertEquals(BRANCA, game.getTurnoAtual());
        assertEquals(new Board().get(pos("e2")), game.getBoard().get(pos("e2")));
    }

    @Test
    void lanceValidoMoveAPecaEPassaAVez() {
        Game game = service.createGame();

        service.move(game.getId(), pos("g1"), pos("f3"));

        assertNull(game.getBoard().get(pos("g1")));
        assertEquals(PieceType.CAVALO, game.getBoard().get(pos("f3")).type());
        assertEquals(PRETA, game.getTurnoAtual());
    }

    @Test
    void iaNaoJogaEmPartidaEncerrada() {
        Game game = service.createGame();
        game.setStatus(GameStatus.XEQUE_MATE);

        assertThrows(GameOverException.class, () -> service.playAiMove(game.getId()));
    }

    @Test
    void iaSemLancesEmAfogamentoEncerraComEmpate() {
        Game game = service.createGame();
        Board board = game.getBoard();
        clearBoard(board);
        put(board, "h1", PieceType.REI, BRANCA);
        put(board, "g3", PieceType.RAINHA, PRETA);
        put(board, "a8", PieceType.REI, PRETA);

        service.playAiMove(game.getId());

        assertEquals(GameStatus.EMPATE, game.getStatus());
        assertNull(game.getVencedor());
    }

    @Test
    void iaSemLancesEmXequeMateEncerraComVitoriaDoAdversario() {
        Game game = service.createGame();
        Board board = game.getBoard();
        clearBoard(board);
        put(board, "h1", PieceType.REI, BRANCA);
        put(board, "g2", PieceType.RAINHA, PRETA);
        put(board, "f3", PieceType.REI, PRETA);

        service.playAiMove(game.getId());

        assertEquals(GameStatus.XEQUE_MATE, game.getStatus());
        assertEquals(PRETA, game.getVencedor());
    }

    private void clearBoard(Board board) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board.set(row, col, null);
            }
        }
    }
}
