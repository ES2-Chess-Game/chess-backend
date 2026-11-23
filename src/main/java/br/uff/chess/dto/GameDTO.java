package br.uff.chess.dto;

import java.util.List;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.GameSettings;
import br.uff.chess.model.PlayedMove;
import br.uff.chess.model.Piece;

public record GameDTO(String id, PieceDTO[][] tabuleiro, Color turnoAtual, GameStatus status, Color vencedor,
        Long userId, GameSettings configuracao, List<PlayedMove> lances, long criadaEm, Long encerradaEm,
        Long brancasMs, Long pretasMs) {

    public static GameDTO from(Game game) {
        Board board = game.getBoard();
        PieceDTO[][] tabuleiro = new PieceDTO[Board.SIZE][Board.SIZE];
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                tabuleiro[row][col] = piece == null ? null : PieceDTO.from(piece);
            }
        }
        long now = System.currentTimeMillis();
        return new GameDTO(game.getId(), tabuleiro, game.getTurnoAtual(), game.getStatus(), game.getVencedor(),
                game.getUserId(), game.getSettings(), game.getMoves(), game.getCreatedAt(), game.getFinishedAt(),
                game.clockRemaining(Color.BRANCA, now), game.clockRemaining(Color.PRETA, now));
    }
}
