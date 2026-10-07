package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;
import br.uff.chess.service.rules.LegalMoveGenerator;

/**
 * Testa a integração do fim de jogo em GameService. Os cenários de
 * xeque-mate/afogamento usam stubs de CheckDetector/LegalMoveGenerator no
 * avaliador de fim de jogo, enquanto a validação do lance usa a implementação real.
 */
class GameServiceEndGameTest {

    private static final Position ORIGEM_PEAO_BRANCO = new Position(6, 0);
    private static final Position DESTINO_PEAO_BRANCO = new Position(5, 0);

    private static final DefaultLegalMoveGenerator REAL_GENERATOR =
            new DefaultLegalMoveGenerator(new DefaultCheckDetector());

    private static GameService realService() {
        return new GameService(new DefaultCheckDetector(), REAL_GENERATOR);
    }

    @Test
    void bloqueiaLanceAposXequeMate() {
        GameService service = realService();
        Game game = service.createGame();
        game.setStatus(GameStatus.XEQUE_MATE);

        assertThrows(GameOverException.class,
                () -> service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO));
    }

    @Test
    void bloqueiaLanceAposEmpatePorAfogamento() {
        GameService service = realService();
        Game game = service.createGame();
        game.setStatus(GameStatus.EMPATE);

        assertThrows(GameOverException.class,
                () -> service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO));
    }

    @Test
    void lanceInicialComDeteccaoRealMantemPartidaEmAndamento() {
        GameService service = realService();
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.EM_ANDAMENTO, atualizado.getStatus());
        assertNull(atualizado.getVencedor());
    }

    @Test
    void marcaXequeMateEDefineVencedorQuandoDependenciaIndicaFimDeJogo() {
        CheckDetector sempreEmXeque = (board, color) -> true;
        LegalMoveGenerator semLanceLegal = (board, color) -> false;
        GameService service = new GameService(new EndGameEvaluator(sempreEmXeque, semLanceLegal), REAL_GENERATOR, sempreEmXeque);
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.XEQUE_MATE, atualizado.getStatus());
        assertEquals(Color.BRANCA, atualizado.getVencedor());
    }

    @Test
    void marcaEmpateQuandoDependenciaIndicaAfogamento() {
        CheckDetector nuncaEmXeque = (board, color) -> false;
        LegalMoveGenerator semLanceLegal = (board, color) -> false;
        GameService service = new GameService(new EndGameEvaluator(nuncaEmXeque, semLanceLegal), REAL_GENERATOR, nuncaEmXeque);
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.EMPATE, atualizado.getStatus());
        assertNull(atualizado.getVencedor());
    }

    @Test
    void jogadorEmXequeComSaidaLegalMantemPartidaComoXeque() {
        CheckDetector emXeque = (board, color) -> true;
        LegalMoveGenerator haLanceLegal = (board, color) -> true;
        GameService service = new GameService(new EndGameEvaluator(emXeque, haLanceLegal), REAL_GENERATOR, emXeque);
        Game game = service.createGame();

        Game atualizado = service.move(game.getId(), ORIGEM_PEAO_BRANCO, DESTINO_PEAO_BRANCO);

        assertEquals(GameStatus.XEQUE, atualizado.getStatus());
        assertNull(atualizado.getVencedor());
    }
}
