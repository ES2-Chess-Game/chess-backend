package br.uff.chess.dto;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameStatus;
import br.uff.chess.model.Piece;

public record GameDTO(String id, PieceDTO[][] tabuleiro, Color turnoAtual, GameStatus status) {

    public static GameDTO from(Game game) {
        Board board = game.getBoard();
        PieceDTO[][] tabuleiro = new PieceDTO[Board.SIZE][Board.SIZE];
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                tabuleiro[row][col] = piece == null ? null : PieceDTO.from(piece);
            }
        }
        return new GameDTO(game.getId(), tabuleiro, game.getTurnoAtual(), game.getStatus());
    }
}
