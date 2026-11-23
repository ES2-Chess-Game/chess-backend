package br.uff.chess.dto;

import br.uff.chess.service.LegalMove;

public record LegalMoveResponseDTO(int origemLinha, int origemColuna, int destinoLinha, int destinoColuna) {
    public static LegalMoveResponseDTO from(LegalMove m) {
        return new LegalMoveResponseDTO(m.from().row(), m.from().col(), m.to().row(), m.to().col());
    }
}
