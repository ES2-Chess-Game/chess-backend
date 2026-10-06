package br.uff.chess.service.validator;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Position;

/**
 * Uma casa em qualquer direção. Não verifica se o destino deixa o próprio
 * rei em xeque nem trata roque — fora do escopo atual (sem detecção de xeque).
 */
public class KingValidator implements PieceMoveValidator {

    @Override
    public boolean isValid(Board board, Position from, Position to, Color color) {
        int dr = Math.abs(to.row() - from.row());
        int dc = Math.abs(to.col() - from.col());
        if (dr == 0 && dc == 0) {
            return false;
        }
        boolean oneSquare = dr <= 1 && dc <= 1;
        return oneSquare && !ValidatorUtils.hasOwnPieceAt(board, to, color);
    }
}
