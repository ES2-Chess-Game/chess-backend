package br.uff.chess.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.validator.BishopValidator;
import br.uff.chess.service.validator.KingValidator;
import br.uff.chess.service.validator.KnightValidator;
import br.uff.chess.service.validator.PawnValidator;
import br.uff.chess.service.validator.PieceMoveValidator;
import br.uff.chess.service.validator.QueenValidator;
import br.uff.chess.service.validator.RookValidator;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private final Map<PieceType, PieceMoveValidator> validators = Map.of(
            PieceType.TORRE, new RookValidator(),
            PieceType.CAVALO, new KnightValidator(),
            PieceType.BISPO, new BishopValidator(),
            PieceType.RAINHA, new QueenValidator(),
            PieceType.REI, new KingValidator(),
            PieceType.PEAO, new PawnValidator());

    public Game createGame() {
        Game game = new Game(UUID.randomUUID().toString(), new Board(), Color.BRANCA);
        games.put(game.getId(), game);
        return game;
    }

    public Game getGame(String id) {
        Game game = games.get(id);
        if (game == null) {
            throw new GameNotFoundException(id);
        }
        return game;
    }

    public Game move(String id, Position from, Position to) {
        Game game = getGame(id);

        if (!Board.isInside(from) || !Board.isInside(to)) {
            throw new IllegalMoveException("Posição fora do tabuleiro");
        }
        if (from.equals(to)) {
            throw new IllegalMoveException("Origem e destino são iguais");
        }

        Board board = game.getBoard();
        Piece piece = board.get(from.row(), from.col());
        if (piece == null) {
            throw new IllegalMoveException("Não há peça na posição de origem");
        }
        if (piece.color() != game.getTurnoAtual()) {
            throw new IllegalMoveException("Peça inválida ou não é sua vez");
        }

        PieceMoveValidator validator = validators.get(piece.type());
        if (!validator.isValid(board, from, to, piece.color())) {
            throw new IllegalMoveException("Movimento ilegal");
        }

        board.set(to.row(), to.col(), piece);
        board.set(from.row(), from.col(), null);
        game.alternarTurno();
        return game;
    }
}
