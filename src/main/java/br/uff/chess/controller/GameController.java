package br.uff.chess.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uff.chess.dto.GameDTO;
import br.uff.chess.dto.MoveRequest;
import br.uff.chess.model.Game;
import br.uff.chess.service.GameService;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.IllegalMoveException;

@RestController
@RequestMapping("/api/partidas")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public GameDTO criar() {
        return GameDTO.from(gameService.createGame());
    }

    @GetMapping("/{id}")
    public GameDTO obter(@PathVariable String id) {
        return GameDTO.from(gameService.getGame(id));
    }

    @PostMapping("/{id}/lances")
    public GameDTO mover(@PathVariable String id, @RequestBody MoveRequest req) {
        Game game = gameService.move(id, req.origem(), req.destino());
        return GameDTO.from(game);
    }

    @ExceptionHandler(IllegalMoveException.class)
    public ResponseEntity<Map<String, String>> handleIllegalMove(IllegalMoveException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(GameNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
    }
}
