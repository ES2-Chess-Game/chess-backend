package br.uff.chess.model;

public record GameSummary(String id, GameStatus status, Color vencedor, GameSettings configuracao, long criadaEm,
        Long encerradaEm) {
}
