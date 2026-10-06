package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.*;
import static br.uff.chess.model.Color.*;
import static br.uff.chess.model.PieceType.*;

import org.junit.jupiter.api.Test;
import br.uff.chess.model.*;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

/** Integra as regras reais de xeque, legalidade e fim de jogo. */
class GameServiceEndGameTest {
    private final DefaultCheckDetector detector = new DefaultCheckDetector();
    private final GameService service = new GameService(detector, new DefaultLegalMoveGenerator(detector));

    @Test
    void bloqueiaLanceAposFimDeJogo() {
        for (GameStatus status : new GameStatus[] {GameStatus.XEQUE_MATE, GameStatus.EMPATE}) {
            Game game = service.createGame();
            game.setStatus(status);
            assertThrows(GameOverException.class, () -> move(game, 6, 0, 5, 0));
        }
    }

    @Test
    void lanceInicialMantemPartidaEmAndamento() {
        Game game = service.createGame();
        move(game, 6, 4, 4, 4);
        assertEquals(GameStatus.EM_ANDAMENTO, game.getStatus());
        assertEquals(PRETA, game.getTurnoAtual());
        assertNull(game.getVencedor());
    }

    @Test
    void mateDoLoucoDefineVencedorEBloqueiaProximoLance() {
        Game game = service.createGame();
        move(game, 6, 5, 5, 5);
        move(game, 1, 4, 3, 4);
        move(game, 6, 6, 4, 6);
        move(game, 0, 3, 4, 7);
        assertEquals(GameStatus.XEQUE_MATE, game.getStatus());
        assertEquals(PRETA, game.getVencedor());
        assertThrows(GameOverException.class, () -> move(game, 6, 0, 5, 0));
    }

    @Test
    void marcaXequeEVoltaAoNormalQuandoReiEscapa() {
        Game game = emptyGame();
        Board board = game.getBoard();
        board.set(7, 7, new Piece(REI, BRANCA));
        board.set(0, 4, new Piece(REI, PRETA));
        board.set(6, 0, new Piece(TORRE, BRANCA));
        move(game, 6, 0, 6, 4);
        assertEquals(GameStatus.XEQUE, game.getStatus());
        move(game, 0, 4, 0, 3);
        assertEquals(GameStatus.EM_ANDAMENTO, game.getStatus());
        assertNull(game.getVencedor());
    }

    @Test
    void marcaAfogamentoSemVencedor() {
        Game game = emptyGame();
        Board board = game.getBoard();
        board.set(0, 0, new Piece(REI, PRETA));
        board.set(2, 2, new Piece(REI, BRANCA));
        board.set(3, 2, new Piece(RAINHA, BRANCA));
        move(game, 3, 2, 2, 1);
        assertEquals(GameStatus.EMPATE, game.getStatus());
        assertNull(game.getVencedor());
    }

    @Test
    void capturaQueExpoeReiNaoAlteraTabuleiroTurnoOuStatus() {
        Game game = emptyGame();
        Board board = game.getBoard();
        board.set(7, 4, new Piece(REI, BRANCA));
        board.set(0, 0, new Piece(REI, PRETA));
        board.set(0, 4, new Piece(TORRE, PRETA));
        Piece pinned = new Piece(TORRE, BRANCA);
        Piece target = new Piece(CAVALO, PRETA);
        board.set(6, 4, pinned);
        board.set(6, 5, target);
        assertThrows(IllegalMoveException.class, () -> move(game, 6, 4, 6, 5));
        assertSame(pinned, board.get(6, 4));
        assertSame(target, board.get(6, 5));
        assertEquals(BRANCA, game.getTurnoAtual());
        assertEquals(GameStatus.EM_ANDAMENTO, game.getStatus());
        assertNull(game.getVencedor());
    }

    private Game emptyGame() {
        Game game = service.createGame();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                game.getBoard().set(row, col, null);
            }
        }
        return game;
    }

    private void move(Game game, int row, int col, int targetRow, int targetCol) {
        service.move(game.getId(), new Position(row, col), new Position(targetRow, targetCol));
    }
}
