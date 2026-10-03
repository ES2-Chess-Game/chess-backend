package br.uff.chess.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.Piece;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.model.GameStatus;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private final LegalMoveService legalMoveService;
    private final MinimaxAI minimaxAI;

    public GameService(LegalMoveService legalMoveService, MinimaxAI minimaxAI) {
        this.legalMoveService = legalMoveService;
        this.minimaxAI = minimaxAI;
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
        validateOngoingGame(game);

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

        if (!legalMoveService.isLegalMove(board, from, to, piece.color())) {
            throw new IllegalMoveException("Movimento ilegal");
        }

        applyMove(game, from, to);
        return game;
    }

    public Game playAiMove(String id) {
        Game game = getGame(id);
        validateOngoingGame(game);

        Color turn = game.getTurnoAtual();
        var move = minimaxAI.chooseMove(game.getBoard(), turn);
        if (move.isEmpty()) {
            updateStatus(game);
            return game;
        }

        applyMove(game, move.get().from(), move.get().to());
        return game;
    }

    private void applyMove(Game game, Position from, Position to) {
        LegalMoveService.applyMove(game.getBoard(), from, to);
        game.alternarTurno();
        updateStatus(game);
    }

    private void updateStatus(Game game) {
        var legalMoves = legalMoveService.getLegalMoves(game.getBoard(), game.getTurnoAtual());
        if (legalMoves.isEmpty()) {
            GameStatus status = legalMoveService.isKingInCheck(game.getBoard(), game.getTurnoAtual())
                    ? GameStatus.XEQUE_MATE
                    : GameStatus.EMPATE;
            game.setStatus(status);
        }
    }

    private void validateOngoingGame(Game game) {
        if (game.getStatus() != GameStatus.EM_ANDAMENTO) {
            throw new IllegalMoveException("A partida já foi encerrada");
        }
    }
}
