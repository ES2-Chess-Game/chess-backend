package br.uff.chess.service.rules;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;

/** Consulta de existência de movimentos que não deixam o próprio rei em xeque. */
public interface LegalMoveGenerator {
    boolean hasLegalMove(Board board, Color color);
}
