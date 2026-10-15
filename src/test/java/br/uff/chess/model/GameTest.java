package br.uff.chess.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GameTest {

    private final Board board = new Board();
    private final Game game = new Game("id-1", board, Color.BRANCA);

    @Test
    void partidaNovaComecaEmAndamentoSemVencedorNemEnPassant() {
        assertEquals("id-1", game.getId());
        assertSame(board, game.getBoard());
        assertEquals(Color.BRANCA, game.getTurnoAtual());
        assertEquals(GameStatus.EM_ANDAMENTO, game.getStatus());
        assertNull(game.getVencedor());
        assertNull(game.getEnPassantTarget());
        assertNull(game.getUserId());
        assertFalse(game.isFinalizada());
    }

    @Test
    void partidaNovaTemTodosOsDireitosDeRoque() {
        for (Color color : Color.values()) {
            assertTrue(game.getCastlingRights().canKingSide(color));
            assertTrue(game.getCastlingRights().canQueenSide(color));
        }
    }

    @Test
    void alternarTurnoTrocaEntreBrancasEPretas() {
        game.alternarTurno();
        assertEquals(Color.PRETA, game.getTurnoAtual());

        game.alternarTurno();
        assertEquals(Color.BRANCA, game.getTurnoAtual());
    }

    @ParameterizedTest
    @EnumSource(value = GameStatus.class, names = {"XEQUE_MATE", "EMPATE"})
    void xequeMateEEmpateFinalizamAPartida(GameStatus status) {
        game.setStatus(status);
        assertTrue(game.isFinalizada());
    }

    @ParameterizedTest
    @EnumSource(value = GameStatus.class, names = {"EM_ANDAMENTO", "XEQUE"})
    void emAndamentoEXequeNaoFinalizamAPartida(GameStatus status) {
        game.setStatus(status);
        assertFalse(game.isFinalizada());
    }

}
