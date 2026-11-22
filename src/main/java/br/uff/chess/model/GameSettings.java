package br.uff.chess.model;

import br.uff.chess.service.AiDifficulty;
import br.uff.chess.service.exceptions.IllegalMoveException;

public record GameSettings(String modo, AiDifficulty dificuldade, String tempo, String brancas, String pretas) {

    public GameSettings {
        modo = modo == null ? "local" : modo;
        tempo = tempo == null ? "0" : tempo;
        if (!modo.equals("local") && !modo.equals("ai")) {
            throw new IllegalMoveException("Modo de partida inválido");
        }
        if (!tempo.equals("0") && !tempo.equals("15") && !tempo.equals("30") && !tempo.equals("60")
                && !tempo.equals("match")) {
            throw new IllegalMoveException("Controle de tempo inválido");
        }
        brancas = playerName(brancas);
        pretas = playerName(pretas);
    }

    public long durationMs() {
        return tempo.equals("match") ? 600_000 : Long.parseLong(tempo) * 1000;
    }

    private static String playerName(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.strip();
        if (normalized.length() > 32) {
            throw new IllegalMoveException("O nome deve ter até 32 caracteres");
        }
        return normalized;
    }
}
