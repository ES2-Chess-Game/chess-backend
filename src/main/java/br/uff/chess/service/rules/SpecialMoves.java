package br.uff.chess.service.rules;

import java.util.List;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.IllegalMoveException;

/**
 * Regras e execução das jogadas especiais: roque, en passant e promoção.
 * Coordenadas: linha 0 = fileira 8 (pretas), linha 7 = fileira 1 (brancas).
 */
public final class SpecialMoves {

    private static final int KING_COL = 4;
    private static final List<PieceType> PROMOTION_TYPES =
            List.of(PieceType.RAINHA, PieceType.TORRE, PieceType.BISPO, PieceType.CAVALO);

    private SpecialMoves() {
    }

    private static int homeRow(Color color) {
        return color == Color.BRANCA ? 7 : 0;
    }

    private static int direction(Color color) {
        return color == Color.BRANCA ? -1 : 1;
    }

    private static Color opponent(Color color) {
        return color == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }

    // ------------------------------------------------------------------ roque

    /** O rei tentou andar duas casas na horizontal (só o roque permite isso). */
    public static boolean isCastlingAttempt(Piece piece, Position from, Position to) {
        return piece.type() == PieceType.REI
                && from.row() == to.row()
                && Math.abs(to.col() - from.col()) == 2;
    }

    /**
     * Roque válido: rei e torre ainda não se moveram, casas entre eles vazias, e o
     * rei não está em xeque nem passa/chega a uma casa atacada.
     */
    public static boolean canCastle(Game game, Position from, Position to) {
        Board board = game.getBoard();
        Piece king = board.get(from);
        Color color = king.color();
        int home = homeRow(color);
        if (from.row() != home || from.col() != KING_COL) {
            return false;
        }

        boolean kingSide = to.col() > from.col();
        boolean hasRight = kingSide
                ? game.getCastlingRights().canKingSide(color)
                : game.getCastlingRights().canQueenSide(color);
        if (!hasRight) {
            return false;
        }

        int rookCol = kingSide ? 7 : 0;
        Piece rook = board.get(home, rookCol);
        if (rook == null || rook.type() != PieceType.TORRE || rook.color() != color) {
            return false;
        }

        int step = kingSide ? 1 : -1;
        for (int col = KING_COL + step; col != rookCol; col += step) {
            if (board.get(home, col) != null) {
                return false;
            }
        }

        Color enemy = opponent(color);
        for (int i = 0; i <= 2; i++) { // casa do rei, casa intermediária e destino
            if (AttackedSquares.isAttacked(board, new Position(home, KING_COL + step * i), enemy)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------- en passant

    /**
     * Peão anda uma casa na diagonal para a casa "pulada" pelo peão adversário no
     * lance imediatamente anterior, capturando esse peão (que está ao lado).
     */
    public static boolean isEnPassant(Game game, Piece piece, Position from, Position to) {
        if (piece.type() != PieceType.PEAO || !to.equals(game.getEnPassantTarget())) {
            return false;
        }
        if (to.row() - from.row() != direction(piece.color()) || Math.abs(to.col() - from.col()) != 1) {
            return false;
        }
        Board board = game.getBoard();
        if (board.get(to) != null) {
            return false;
        }
        Piece captured = board.get(from.row(), to.col());
        return captured != null && captured.type() == PieceType.PEAO && captured.color() != piece.color();
    }

    // --------------------------------------------------------------- promoção

    public static boolean isPromotion(Piece piece, Position to) {
        return piece.type() == PieceType.PEAO && to.row() == homeRow(opponent(piece.color()));
    }

    /**
     * Define a peça resultante da promoção (dama por padrão). Retorna null quando o
     * lance não é uma promoção. Lança exceção se a peça pedida for inválida ou se foi
     * pedida uma promoção em um lance que não promove.
     */
    public static PieceType resolvePromotion(Piece piece, Position to, PieceType requested) {
        if (!isPromotion(piece, to)) {
            if (requested != null) {
                throw new IllegalMoveException("Promoção só é permitida quando o peão chega à última fileira");
            }
            return null;
        }
        if (requested == null) {
            return PieceType.RAINHA;
        }
        if (!PROMOTION_TYPES.contains(requested)) {
            throw new IllegalMoveException("Promoção inválida: escolha rainha, torre, bispo ou cavalo");
        }
        return requested;
    }

    // ---------------------------------------------------------------- execução

    /**
     * Aplica o lance (já validado) ao tabuleiro e atualiza direitos de roque e a
     * casa de en passant. Não alterna o turno.
     */
    public static void apply(Game game, Piece piece, Position from, Position to,
                             boolean castling, boolean enPassant, PieceType promotion) {
        Board board = game.getBoard();

        if (castling) {
            boolean kingSide = to.col() > from.col();
            int rookFrom = kingSide ? 7 : 0;
            int rookTo = kingSide ? 5 : 3;
            board.set(from.row(), rookTo, board.get(from.row(), rookFrom));
            board.set(from.row(), rookFrom, null);
        } else if (enPassant) {
            board.set(from.row(), to.col(), null); // o peão capturado não está na casa de destino
        }

        board.set(to, promotion == null ? piece : new Piece(promotion, piece.color()));
        board.set(from, null);

        if (piece.type() == PieceType.REI) {
            game.getCastlingRights().revokeAll(piece.color());
        }
        game.getCastlingRights().revokeForSquare(from);
        game.getCastlingRights().revokeForSquare(to);

        boolean doublePush = piece.type() == PieceType.PEAO && Math.abs(to.row() - from.row()) == 2;
        game.setEnPassantTarget(doublePush
                ? new Position(from.row() + direction(piece.color()), from.col())
                : null);
    }
}
