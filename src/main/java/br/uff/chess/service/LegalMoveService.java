package br.uff.chess.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

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

@Service
public class LegalMoveService {

    private final Map<PieceType, PieceMoveValidator> validators = new EnumMap<>(PieceType.class);

    public LegalMoveService() {
        validators.put(PieceType.TORRE, new RookValidator());
        validators.put(PieceType.CAVALO, new KnightValidator());
        validators.put(PieceType.BISPO, new BishopValidator());
        validators.put(PieceType.RAINHA, new QueenValidator());
        validators.put(PieceType.REI, new KingValidator());
        validators.put(PieceType.PEAO, new PawnValidator());
    }

    public boolean isLegalMove(Board board, Position from, Position to, Color color) {
        if (!Board.isInside(from) || !Board.isInside(to) || from.equals(to)) {
            return false;
        }

        Piece piece = board.get(from);
        Piece target = board.get(to);
        if (piece == null || piece.color() != color || (target != null && target.type() == PieceType.REI)) {
            return false;
        }

        PieceMoveValidator validator = validators.get(piece.type());
        if (!validator.isValid(board, from, to, color)) {
            return false;
        }

        Board nextBoard = board.copy();
        applyMove(nextBoard, from, to);
        return !isKingInCheck(nextBoard, color);
    }

    public List<LegalMove> getLegalMoves(Board board, Color color) {
        List<LegalMove> moves = new ArrayList<>();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece == null || piece.color() != color) {
                    continue;
                }
                Position from = new Position(row, col);
                for (int targetRow = 0; targetRow < Board.SIZE; targetRow++) {
                    for (int targetCol = 0; targetCol < Board.SIZE; targetCol++) {
                        Position to = new Position(targetRow, targetCol);
                        if (isLegalMove(board, from, to, color)) {
                            moves.add(new LegalMove(from, to));
                        }
                    }
                }
            }
        }
        return moves;
    }

    public boolean isKingInCheck(Board board, Color color) {
        Position kingPosition = findKing(board, color);
        if (kingPosition == null) {
            return true;
        }

        Color opponent = opponentOf(color);
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece == null || piece.color() != opponent) {
                    continue;
                }
                PieceMoveValidator validator = validators.get(piece.type());
                if (validator.isValid(board, new Position(row, col), kingPosition, opponent)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Color opponentOf(Color color) {
        return color == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }

    public static void applyMove(Board board, Position from, Position to) {
        board.set(to, board.get(from));
        board.set(from, null);
    }

    private Position findKing(Board board, Color color) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece != null && piece.type() == PieceType.REI && piece.color() == color) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }
}