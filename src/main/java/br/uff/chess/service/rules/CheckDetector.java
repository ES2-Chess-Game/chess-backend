package br.uff.chess.service.rules;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;

/** Consulta de xeque, independente do serviço de partidas. */
public interface CheckDetector {
    boolean isKingInCheck(Board board, Color color);
}
