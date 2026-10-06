package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Position;

/**
 * Rainha = Torre + Bispo combinados.
 */
public class QueenValidator implements PieceMoveValidator {

    private final RookValidator rook = new RookValidator();
    private final BishopValidator bishop = new BishopValidator();

    @Override
    public boolean isValid(Board board, Position from, Position to, Color color) {
        return rook.isValid(board, from, to, color) || bishop.isValid(board, from, to, color);
    }
}
