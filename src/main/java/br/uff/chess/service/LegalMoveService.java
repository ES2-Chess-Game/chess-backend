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

    private static final List<PieceType> PROMOTION_TYPES = List.of(
            PieceType.RAINHA, PieceType.TORRE, PieceType.BISPO, PieceType.CAVALO);

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

        if (isCastlingMove(piece, from, to)) {
            if (!isLegalCastle(board, from, to, color)) {
                return false;
            }
        } else {
            PieceMoveValidator validator = validators.get(piece.type());
            if (!validator.isValid(board, from, to, color)) {
                return false;
            }
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
                            if (piece.type() == PieceType.PEAO &&
                                    (targetRow == 0 || targetRow == Board.SIZE - 1)) {
                                for (PieceType promotion : PROMOTION_TYPES) {
                                    moves.add(new LegalMove(from, to, promotion));
                                }
                            } else {
                                moves.add(new LegalMove(from, to));
                            }
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
        applyMove(board, from, to, null);
    }

    public static void applyMove(Board board, Position from, Position to, PieceType promotion) {
        Piece movingPiece = board.get(from);
        Piece capturedPiece = board.get(to);
        boolean enPassantCapture = movingPiece.type() == PieceType.PEAO &&
                from.col() != to.col() && capturedPiece == null;
        boolean castling = isCastlingMove(movingPiece, from, to);

        if (movingPiece.type() == PieceType.REI) {
            board.revokeCastlingRights(movingPiece.color());
        } else if (movingPiece.type() == PieceType.TORRE) {
            revokeRookRight(board, movingPiece.color(), from);
        }
        if (capturedPiece != null && capturedPiece.type() == PieceType.TORRE) {
            revokeRookRight(board, capturedPiece.color(), to);
        }

        if (enPassantCapture) {
            board.set(from.row(), to.col(), null);
        }
        if (castling) {
            int rookFromCol = to.col() == 6 ? 7 : 0;
            int rookToCol = to.col() == 6 ? 5 : 3;
            board.set(to.row(), rookToCol, board.get(to.row(), rookFromCol));
            board.set(to.row(), rookFromCol, null);
        }

        Piece movedPiece = movingPiece;
        if (movingPiece.type() == PieceType.PEAO && (to.row() == 0 || to.row() == Board.SIZE - 1)) {
            movedPiece = new Piece(promotion == null ? PieceType.RAINHA : promotion, movingPiece.color());
        }

        board.setEnPassantTarget(null);
        if (movingPiece.type() == PieceType.PEAO && Math.abs(to.row() - from.row()) == 2) {
            board.setEnPassantTarget(new Position((from.row() + to.row()) / 2, from.col()));
        }

        board.set(to, movedPiece);
        board.set(from, null);
    }

    private boolean isLegalCastle(Board board, Position from, Position to, Color color) {
        int homeRow = color == Color.BRANCA ? Board.SIZE - 1 : 0;
        if (from.row() != homeRow || from.col() != 4 || to.row() != homeRow ||
                (to.col() != 2 && to.col() != 6)) {
            return false;
        }

        boolean kingSide = to.col() == 6;
        int rookCol = kingSide ? 7 : 0;
        Piece rook = board.get(homeRow, rookCol);
        if (!board.canCastle(color, kingSide) || rook == null ||
                rook.type() != PieceType.TORRE || rook.color() != color) {
            return false;
        }

        int firstCol = kingSide ? 5 : 1;
        int lastCol = kingSide ? 6 : 3;
        for (int col = firstCol; col <= lastCol; col++) {
            if (board.get(homeRow, col) != null) {
                return false;
            }
        }
        if (isKingInCheck(board, color)) {
            return false;
        }

        int transitCol = kingSide ? 5 : 3;
        Board transitBoard = board.copy();
        transitBoard.set(from, null);
        transitBoard.set(homeRow, transitCol, new Piece(PieceType.REI, color));
        return !isKingInCheck(transitBoard, color);
    }

    private static boolean isCastlingMove(Piece piece, Position from, Position to) {
        return piece.type() == PieceType.REI && from.row() == to.row() &&
                Math.abs(to.col() - from.col()) == 2;
    }

    private static void revokeRookRight(Board board, Color color, Position position) {
        int homeRow = color == Color.BRANCA ? Board.SIZE - 1 : 0;
        if (position.row() != homeRow) {
            return;
        }
        if (position.col() == 0) {
            board.revokeCastlingRight(color, false);
        } else if (position.col() == Board.SIZE - 1) {
            board.revokeCastlingRight(color, true);
        }
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