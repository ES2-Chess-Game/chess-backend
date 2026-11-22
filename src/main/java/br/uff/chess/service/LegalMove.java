package br.uff.chess.service;

import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

public record LegalMove(Position from, Position to, PieceType promotion) {
    public LegalMove(Position from, Position to) {
        this(from, to, null);
    }
}