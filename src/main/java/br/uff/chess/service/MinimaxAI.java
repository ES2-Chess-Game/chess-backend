package br.uff.chess.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;

@Service
public class MinimaxAI {

    private static final int SEARCH_DEPTH = 3;
    private static final int MATE_SCORE = 100_000;

    private final LegalMoveService legalMoveService;

    public MinimaxAI(LegalMoveService legalMoveService) {
        this.legalMoveService = legalMoveService;
    }

    public Optional<LegalMove> chooseMove(Board board, Color color) {
        List<LegalMove> moves = legalMoveService.getLegalMoves(board, color);
        if (moves.isEmpty()) {
            return Optional.empty();
        }

        LegalMove bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        for (LegalMove move : moves) {
            Board nextBoard = board.copy();
            LegalMoveService.applyMove(nextBoard, move.from(), move.to());
            int score = minimax(nextBoard, LegalMoveService.opponentOf(color), SEARCH_DEPTH - 1,
                    1, alpha, beta, color);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestScore);
        }
        return Optional.of(bestMove);
    }

    private int minimax(Board board, Color turn, int depth, int ply, int alpha, int beta, Color perspective) {
        List<LegalMove> moves = legalMoveService.getLegalMoves(board, turn);
        if (moves.isEmpty()) {
            if (!legalMoveService.isKingInCheck(board, turn)) {
                return 0;
            }
            return turn == perspective ? -MATE_SCORE + ply : MATE_SCORE - ply;
        }
        if (depth == 0) {
            return evaluate(board, perspective);
        }

        boolean maximizing = turn == perspective;
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (LegalMove move : moves) {
            Board nextBoard = board.copy();
            LegalMoveService.applyMove(nextBoard, move.from(), move.to());
            int score = minimax(nextBoard, LegalMoveService.opponentOf(turn), depth - 1,
                    ply + 1, alpha, beta, perspective);
            if (maximizing) {
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);
            } else {
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);
            }
            if (beta <= alpha) {
                break;
            }
        }
        return bestScore;
    }

    private int evaluate(Board board, Color perspective) {
        int score = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece == null) {
                    continue;
                }
                int value = valueOf(piece.type());
                score += piece.color() == perspective ? value : -value;
            }
        }
        return score;
    }

    private int valueOf(PieceType type) {
        return switch (type) {
            case PEAO -> 100;
            case CAVALO -> 320;
            case BISPO -> 330;
            case TORRE -> 500;
            case RAINHA -> 900;
            case REI -> 0;
        };
    }
}