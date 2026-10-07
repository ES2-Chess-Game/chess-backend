package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.*;
import static br.uff.chess.model.Color.*;
import static br.uff.chess.model.PieceType.*;

import org.junit.jupiter.api.Test;

import br.uff.chess.dto.GameDTO;
import br.uff.chess.model.*;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

/** Testes do serviço usando as implementações reais de xeque e legalidade. */
class GameServiceLegalityTest {
    private final DefaultCheckDetector detector = new DefaultCheckDetector();
    private final GameService service = new GameService(detector, new DefaultLegalMoveGenerator(detector));

    @Test
    void lanceInicialMantemPartidaEmAndamentoEAlternaTurno() {
        Game game = service.createGame();
        move(game, 6, 4, 4, 4);
        assertEquals(GameStatus.EM_ANDAMENTO, game.getStatus());
        assertEquals(PRETA, game.getTurnoAtual());
        assertNull(game.getBoard().get(6, 4));
        assertEquals(new Piece(PEAO, BRANCA), game.getBoard().get(4, 4));
    }

    @Test
    void marcaXequeNoDtoEVoltaAoNormalQuandoReiEscapa() {
        Game game = emptyGame();
        Board board = game.getBoard();
        board.set(7, 7, new Piece(REI, BRANCA));
        board.set(0, 4, new Piece(REI, PRETA));
        board.set(6, 0, new Piece(TORRE, BRANCA));
        move(game, 6, 0, 6, 4);
        assertEquals(GameStatus.XEQUE, GameDTO.from(game).status());
        move(game, 0, 4, 0, 3);
        assertEquals(GameStatus.EM_ANDAMENTO, GameDTO.from(game).status());
        assertEquals(BRANCA, game.getTurnoAtual());
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
    }

    @Test
    void recusaIgnorarXequeEMantemStatus() {
        Game game = emptyGame();
        Board board = game.getBoard();
        board.set(7, 7, new Piece(REI, BRANCA));
        board.set(0, 4, new Piece(REI, PRETA));
        board.set(6, 0, new Piece(TORRE, BRANCA));
        board.set(1, 0, new Piece(PEAO, PRETA));
        move(game, 6, 0, 6, 4);
        assertThrows(IllegalMoveException.class, () -> move(game, 1, 0, 2, 0));
        assertEquals(GameStatus.XEQUE, game.getStatus());
        assertEquals(PRETA, game.getTurnoAtual());
        assertEquals(new Piece(PEAO, PRETA), board.get(1, 0));
        assertNull(board.get(2, 0));
    }

    @Test
    void reiNaoPodeCapturarPecaProtegida() {
        Game game = emptyGame();
        Board board = game.getBoard();
        board.set(0, 7, new Piece(REI, PRETA));
        board.set(4, 4, new Piece(REI, BRANCA));
        board.set(2, 2, new Piece(PEAO, PRETA));
        Piece target = new Piece(CAVALO, PRETA);
        board.set(3, 3, target);
        assertThrows(IllegalMoveException.class, () -> move(game, 4, 4, 3, 3));
        assertSame(target, board.get(3, 3));
        assertEquals(new Piece(REI, BRANCA), board.get(4, 4));
        assertEquals(BRANCA, game.getTurnoAtual());
    }

    @Test
    void pecasPretasTambemNaoPodemExporOProprioRei() {
        Game game = emptyGame();
        Board board = game.getBoard();
        board.set(7, 0, new Piece(REI, BRANCA));
        board.set(7, 4, new Piece(TORRE, BRANCA));
        board.set(0, 4, new Piece(REI, PRETA));
        board.set(1, 4, new Piece(TORRE, PRETA));
        move(game, 7, 0, 6, 0);
        assertThrows(IllegalMoveException.class, () -> move(game, 1, 4, 1, 5));
        assertEquals(PRETA, game.getTurnoAtual());
        assertEquals(new Piece(TORRE, PRETA), board.get(1, 4));
        assertNull(board.get(1, 5));
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
