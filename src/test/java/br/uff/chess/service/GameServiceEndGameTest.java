package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.LegalMoveGenerator;

/**
 * Testa a integração do fim de jogo em GameService. Como a implementação real
 * de CheckDetector/LegalMoveGenerator ainda não existe no projeto (é de outro
 * integrante), os cenários de xeque-mate/afogamento aqui usam stubs para
 * simular o resultado da dependência — não reimplementam detecção de xeque.
 */
class GameServiceEndGameTest {

    private static final Position ORIGEM_PEAO_BRANCO = new Position(6, 0);
    private static final Position DESTINO_PEAO_BRANCO = new Position(5, 0);

    @Test
    void bloqueiaLanceAposXequeMate() {
        GameService service = new GameService(Optional.empty(), Optional.empty());
        Game game = service.createGame();
        game.setStatus(GameStatus.XEQUE_MATE);

        assertThrows(GameOverException.class,
                () -> service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO));
    }

    @Test
    void bloqueiaLanceAposEmpatePorAfogamento() {
        GameService service = new GameService(Optional.empty(), Optional.empty());
        Game game = service.createGame();
        game.setStatus(GameStatus.EMPATE);

        assertThrows(GameOverException.class,
                () -> service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO));
    }

    @Test
    void semDependenciaDeXequeDisponivelPartidaSeguePartidaEmAndamento() {
        GameService service = new GameService(Optional.empty(), Optional.empty());
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.EM_ANDAMENTO, atualizado.getStatus());
        assertNull(atualizado.getVencedor());
    }

    @Test
    void marcaXequeMateEDefineVencedorQuandoDependenciaIndicaFimDeJogo() {
        CheckDetector sempreEmXeque = (board, color) -> true;
        LegalMoveGenerator semLanceLegal = (board, color) -> false;
        GameService service = new GameService(Optional.of(sempreEmXeque), Optional.of(semLanceLegal));
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.XEQUE_MATE, atualizado.getStatus());
        assertEquals(Color.BRANCA, atualizado.getVencedor());
    }

    @Test
    void marcaEmpateQuandoDependenciaIndicaAfogamento() {
        CheckDetector nuncaEmXeque = (board, color) -> false;
        LegalMoveGenerator semLanceLegal = (board, color) -> false;
        GameService service = new GameService(Optional.of(nuncaEmXeque), Optional.of(semLanceLegal));
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.EMPATE, atualizado.getStatus());
        assertNull(atualizado.getVencedor());
    }

    @Test
    void jogadorEmXequeComSaidaLegalMantemPartidaComoXeque() {
        CheckDetector emXeque = (board, color) -> true;
        LegalMoveGenerator haLanceLegal = (board, color) -> true;
        GameService service = new GameService(Optional.of(emXeque), Optional.of(haLanceLegal));
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.XEQUE, atualizado.getStatus());
        assertNull(atualizado.getVencedor());
    }
}
