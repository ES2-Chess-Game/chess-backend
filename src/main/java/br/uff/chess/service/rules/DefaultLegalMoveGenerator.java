package br.uff.chess.service.rules;

import java.util.Map;

import org.springframework.stereotype.Component;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.validator.BishopValidator;
import br.uff.chess.service.validator.KingValidator;
import br.uff.chess.service.validator.KnightValidator;
import br.uff.chess.service.validator.PawnValidator;
import br.uff.chess.service.validator.PieceMoveValidator;
import br.uff.chess.service.validator.QueenValidator;
import br.uff.chess.service.validator.RookValidator;

@Component
public class DefaultLegalMoveGenerator implements LegalMoveGenerator {

    private final CheckDetector checkDetector;
    private final Map<PieceType, PieceMoveValidator> validators = Map.of(
            PieceType.TORRE, new RookValidator(),
            PieceType.CAVALO, new KnightValidator(),
            PieceType.BISPO, new BishopValidator(),
            PieceType.RAINHA, new QueenValidator(),
            PieceType.REI, new KingValidator(),
            PieceType.PEAO, new PawnValidator());

    public DefaultLegalMoveGenerator(CheckDetector checkDetector) {
        this.checkDetector = checkDetector;
    }

    public boolean isLegalMove(Board board, Position from, Position to, Color color) {
        if (!Board.isInside(from) || !Board.isInside(to) || from.equals(to)) {
            return false;
        }
        Piece piece = board.get(from);
        Piece target = board.get(to);
        if (piece == null || piece.color() != color
                || (target != null && (target.color() == color || target.type() == PieceType.REI))) {
            return false;
        }
        if (!validators.get(piece.type()).isValid(board, from, to, color)) {
            return false;
        }

        // Simula em uma cópia: consultas e lances recusados não alteram a partida.
        Board simulated = new Board();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                simulated.set(row, col, board.get(row, col));
            }
        }
        simulated.set(from, null);
        simulated.set(to, piece);
        return !checkDetector.isKingInCheck(simulated, color);
    }

    @Override
    public boolean hasLegalMove(Board board, Color color) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece == null || piece.color() != color) {
                    continue;
                }
                Position from = new Position(row, col);
                for (int targetRow = 0; targetRow < Board.SIZE; targetRow++) {
                    for (int targetCol = 0; targetCol < Board.SIZE; targetCol++) {
                        if (isLegalMove(board, from, new Position(targetRow, targetCol), color)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
