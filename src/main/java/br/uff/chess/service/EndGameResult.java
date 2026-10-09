package br.uff.chess.service;

import br.uff.chess.model.Color;
import br.uff.chess.model.GameStatus;

/**
 * Resultado da avaliação de fim de jogo para a cor que está prestes a jogar.
 * {@code vencedor} só é preenchido em caso de {@link GameStatus#XEQUE_MATE}.
 */
public record EndGameResult(GameStatus status, Color vencedor) {

    public static EndGameResult emAndamento() {
        return new EndGameResult(GameStatus.EM_ANDAMENTO, null);
    }

    public static EndGameResult xeque() {
        return new EndGameResult(GameStatus.XEQUE, null);
    }

    public static EndGameResult xequeMate(Color vencedor) {
        return new EndGameResult(GameStatus.XEQUE_MATE, vencedor);
    }

    public static EndGameResult empate() {
        return new EndGameResult(GameStatus.EMPATE, null);
    }
}
