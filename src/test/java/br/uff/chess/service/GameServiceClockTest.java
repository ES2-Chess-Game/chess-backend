package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

class GameServiceClockTest {

    private static final long DEZ_MIN = 600_000L;

    private static class RelogioMutavel extends Clock {
        private Instant agora = Instant.parse("2026-01-01T00:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioMutavel clock = new RelogioMutavel();
    private final DefaultCheckDetector detector = new DefaultCheckDetector();
    private final GameService service = new GameService(detector, new DefaultLegalMoveGenerator(detector), clock);

    private long restante(Game game, Color cor) {
        return game.restanteMs(cor, service.agora());
    }

    @Test
    void lanceDasBrancasDescontaOTempoDelasEMantemODasPretas() {
        Game game = service.createGame();
        clock.avancar(Duration.ofSeconds(5));

        service.move(game.getId(), new Position(6, 4), new Position(4, 4));

        assertEquals(DEZ_MIN - 5_000, restante(game, Color.BRANCA));
        assertEquals(DEZ_MIN, restante(game, Color.PRETA));
    }

    @Test
    void lanceDasPretasDescontaOTempoDelasEMantemOTempoDasBrancas() {
        Game game = service.createGame();
        clock.avancar(Duration.ofSeconds(5));
        service.move(game.getId(), new Position(6, 4), new Position(4, 4));
        clock.avancar(Duration.ofSeconds(3));

        service.move(game.getId(), new Position(1, 4), new Position(3, 4));

        assertEquals(DEZ_MIN - 5_000, restante(game, Color.BRANCA));
        assertEquals(DEZ_MIN - 3_000, restante(game, Color.PRETA));
    }

    @Test
    void passarDoTempoEMoverEncerraPorTempoEsgotadoComVencedorOposto() {
        Game game = service.createGame();
        clock.avancar(Duration.ofMillis(DEZ_MIN + 1));

        assertThrows(GameOverException.class,
                () -> service.move(game.getId(), new Position(6, 4), new Position(4, 4)));

        assertEquals(GameStatus.TEMPO_ESGOTADO, game.getStatus());
        assertEquals(Color.PRETA, game.getVencedor());
        assertEquals(0, restante(game, Color.BRANCA));
    }

    @Test
    void consultarDepoisDeEstourarOTempoJaDevolveTempoEsgotado() {
        Game game = service.createGame();
        clock.avancar(Duration.ofMinutes(11));

        Game consultada = service.getGame(game.getId());

        assertEquals(GameStatus.TEMPO_ESGOTADO, consultada.getStatus());
        assertEquals(Color.PRETA, consultada.getVencedor());
    }

    @Test
    void tempoExatamenteZeradoContaComoEsgotado() {
        Game game = service.createGame();
        clock.avancar(Duration.ofMillis(DEZ_MIN));

        assertEquals(GameStatus.TEMPO_ESGOTADO, service.getGame(game.getId()).getStatus());
    }

    @Test
    void tempoDasPretasEsgotadoDaVitoriaAsBrancas() {
        Game game = service.createGame();
        service.move(game.getId(), new Position(6, 4), new Position(4, 4));
        clock.avancar(Duration.ofMinutes(10));

        Game consultada = service.getGame(game.getId());

        assertEquals(GameStatus.TEMPO_ESGOTADO, consultada.getStatus());
        assertEquals(Color.BRANCA, consultada.getVencedor());
    }
}
