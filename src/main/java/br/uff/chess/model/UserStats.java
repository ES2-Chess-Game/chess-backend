package br.uff.chess.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class UserStats {

    private int wins;
    private int losses;
    private int draws;

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }

    public void addDraw() {
        draws++;
    }
}
