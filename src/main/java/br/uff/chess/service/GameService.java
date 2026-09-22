package br.uff.chess.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.Piece;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private final EndGameEvaluator endGameEvaluator;
    private final DefaultLegalMoveGenerator legalMoveGenerator;

    @Autowired
    public GameService(CheckDetector checkDetector, DefaultLegalMoveGenerator legalMoveGenerator) {
        this(new EndGameEvaluator(checkDetector, legalMoveGenerator), legalMoveGenerator);
    }

    GameService(EndGameEvaluator endGameEvaluator, DefaultLegalMoveGenerator legalMoveGenerator) {
        this.endGameEvaluator = endGameEvaluator;
        this.legalMoveGenerator = legalMoveGenerator;
    }

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

        if (game.isFinalizada()) {
            throw new GameOverException("A partida já foi encerrada");
        }

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

        if (!legalMoveGenerator.isLegalMove(board, from, to, piece.color())) {
            throw new IllegalMoveException("Movimento ilegal ou deixa o próprio rei em xeque");
        }

        board.set(to.row(), to.col(), piece);
        board.set(from.row(), from.col(), null);
        game.alternarTurno();

        EndGameResult result = endGameEvaluator.evaluate(board, game.getTurnoAtual());
        game.setStatus(result.status());
        if (result.vencedor() != null) {
            game.setVencedor(result.vencedor());
        }
        return game;
    }
}
