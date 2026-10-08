package br.uff.chess.service;

import br.uff.chess.model.Position;

public record LegalMove(Position from, Position to) {
}