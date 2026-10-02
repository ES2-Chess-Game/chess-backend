package br.uff.chess.service.rules;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;

/**
 * Contrato esperado pela lógica de fim de jogo (xeque-mate/afogamento).
 * Um lance só é legal se, além de respeitar o padrão de movimento da peça
 * ({@link br.uff.chess.service.validator.PieceMoveValidator}), não deixar o
 * próprio rei em xeque. Implementação real fica a cargo da funcionalidade de
 * "xeque e legalidade", desenvolvida em paralelo por outro integrante.
 */
public interface LegalMoveGenerator {

    /**
     * @return true se a cor informada possui pelo menos um lance legal disponível
     * na posição atual do tabuleiro.
     */
    boolean hasLegalMove(Board board, Color color);
}
