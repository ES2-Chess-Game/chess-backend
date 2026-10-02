package br.uff.chess.service.rules;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;

/**
 * Contrato esperado pela lógica de fim de jogo (xeque-mate/afogamento).
 * Implementação real fica a cargo da funcionalidade de "xeque e legalidade"
 * (casas ameaçadas, impedir auto-xeque), desenvolvida em paralelo por outro
 * integrante. Até que exista um bean concreto, o fim de jogo não é avaliado
 * automaticamente — ver {@link br.uff.chess.service.GameService}.
 */
public interface CheckDetector {

    /**
     * @return true se o rei da cor informada está em xeque na posição atual do tabuleiro.
     */
    boolean isKingInCheck(Board board, Color color);
}
