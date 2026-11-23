package br.uff.chess.model;

public record PlayedMove(Position origem, Position destino, Color cor, PieceType peca, PieceType promocao,
        PieceType captura) {
}
