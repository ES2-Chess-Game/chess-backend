package br.uff.chess.service;

public enum AiDifficulty {
    EASY(1),
    MEDIUM(2),
    ADVANCED(3);

    private final int searchDepth;

    AiDifficulty(int searchDepth) {
        this.searchDepth = searchDepth;
    }

    public int searchDepth() {
        return searchDepth;
    }
}
