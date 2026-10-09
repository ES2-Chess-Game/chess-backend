package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.GameStatus;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.LegalMoveGenerator;

/**
 * Testa a máquina de estados de fim de jogo isoladamente, usando stubs para
 * CheckDetector e LegalMoveGenerator (cuja implementação real é de outro
 * integrante). O objetivo aqui não é validar detecção de xeque de verdade,
 * apenas a decisão tomada a partir dela.
 */
class EndGameEvaluatorTest {

    private EndGameEvaluator evaluator(boolean emXeque, boolean haLanceLegal) {
        CheckDetector checkDetector = (board, color) -> emXeque;
        LegalMoveGenerator legalMoveGenerator = (board, color) -> haLanceLegal;
        return new EndGameEvaluator(checkDetector, legalMoveGenerator);
    }

    @Test
    void naoEhXequeMateQuandoJogadorEmXequeAindaTemLanceLegal() {
        EndGameResult result = evaluator(true, true).evaluate(new Board(), Color.PRETA);

        assertEquals(GameStatus.XEQUE, result.status());
        assertNull(result.vencedor());
    }

    @Test
    void detectaXequeMateQuandoEmXequeSemNenhumLanceLegal() {
        EndGameResult result = evaluator(true, false).evaluate(new Board(), Color.PRETA);

        assertEquals(GameStatus.XEQUE_MATE, result.status());
        assertEquals(Color.BRANCA, result.vencedor());
    }

    @Test
    void detectaAfogamentoQuandoNaoEstaEmXequeENaoHaLanceLegal() {
        EndGameResult result = evaluator(false, false).evaluate(new Board(), Color.PRETA);

        assertEquals(GameStatus.EMPATE, result.status());
        assertNull(result.vencedor());
    }

    @Test
    void partidaContinuaEmAndamentoQuandoNaoHaXequeEHaLanceLegal() {
        EndGameResult result = evaluator(false, true).evaluate(new Board(), Color.PRETA);

        assertEquals(GameStatus.EM_ANDAMENTO, result.status());
        assertNull(result.vencedor());
    }
}
