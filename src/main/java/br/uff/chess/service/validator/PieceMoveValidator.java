package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Position;

public interface PieceMoveValidator {
    boolean isValid(Board board, Position from, Position to, Color color);
}
