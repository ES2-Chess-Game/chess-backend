package br.uff.chess.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.uff.chess.dto.GameDTO;
import br.uff.chess.dto.LegalMoveResponseDTO;
import br.uff.chess.dto.MoveRequest;
import br.uff.chess.dto.PossibleMoveDTO;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.GameSettings;
import br.uff.chess.model.Position;
import br.uff.chess.service.AiDifficulty;
import br.uff.chess.service.GameService;
import br.uff.chess.service.exceptions.GameNotFoundException;
import br.uff.chess.service.exceptions.GameOverException;
import br.uff.chess.service.exceptions.IllegalMoveException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/partidas")
public class GameController {

    private static final String VISITOR = "chessVisitor";
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public GameDTO criar(Authentication auth, HttpServletRequest request,
            @RequestBody(required = false) GameSettings settings) {
        HttpSession session = request.getSession();
        String visitor = (String) session.getAttribute(VISITOR);
        if (visitor == null) {
            visitor = UUID.randomUUID().toString();
            session.setAttribute(VISITOR, visitor);
        }
        return GameDTO.from(gameService.createGame(userId(auth), visitor, settings));
    }

    @GetMapping("/em-andamento")
    public ResponseEntity<GameDTO> emAndamento(Authentication auth, HttpServletRequest request) {
        return gameService.inProgress(userId(auth), visitorId(request))
                .map(game -> ResponseEntity.ok(GameDTO.from(game)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/historico")
    public List<GameDTO> historico(Authentication auth, HttpServletRequest request) {
        return gameService.history(userId(auth), visitorId(request)).stream().map(GameDTO::from).toList();
    }

    @GetMapping("/historico/{id}")
    public GameDTO detalhe(@PathVariable String id, Authentication auth, HttpServletRequest request) {
        return GameDTO.from(gameService.historyDetail(id, userId(auth), visitorId(request)));
    }

    @PostMapping("/{id}/desistencia")
    public GameDTO desistir(@PathVariable String id, @RequestParam Color cor, Authentication auth,
            HttpServletRequest request) {
        return GameDTO.from(gameService.resign(id, cor, userId(auth), visitorId(request)));
    }

    private Long userId(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof Long id ? id : null;
    }

    private String visitorId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(VISITOR);
    }

    @GetMapping("/{id}")
    public GameDTO obter(@PathVariable String id) {
        return GameDTO.from(gameService.getGame(id));
    }

    @PostMapping("/{id}/lances")
    public GameDTO mover(@PathVariable String id, @RequestBody MoveRequest req) {
        Game game = gameService.move(id, req.origem(), req.destino(), req.promocao());
        return GameDTO.from(game);
    }

    @GetMapping("/recursos")
    public Map<String, Object> recursos() {
        return Map.of("niveisIa", AiDifficulty.values(), "sugestoesDeLances", true, "partidasPersistentes", true);
    }

    @PostMapping("/{id}/ia")
    public GameDTO jogarComIa(@PathVariable String id,
            @RequestParam(defaultValue = "ADVANCED") AiDifficulty difficulty) {
        return GameDTO.from(gameService.playAiMove(id, difficulty));
    }

    @GetMapping({"/{id}/movimentos", "/{id}/movimentos-possiveis"})
    public ResponseEntity<?> obterMovimentosPossiveis(@PathVariable String id,
            @RequestParam(required = false) Integer linha,
            @RequestParam(required = false) Integer coluna) {
        if ((linha == null) != (coluna == null)) {
            throw new IllegalMoveException("Informe linha e coluna da peça");
        }
        if (linha != null) {
            List<PossibleMoveDTO> moves = gameService.getPossibleMoves(id, new Position(linha, coluna))
                    .stream().map(PossibleMoveDTO::from).toList();
            return ResponseEntity.ok(moves);
        }
        List<LegalMoveResponseDTO> moves = gameService.getAllLegalMoves(id)
                .stream().map(LegalMoveResponseDTO::from).toList();
        return ResponseEntity.ok(moves);
    }

    @ExceptionHandler(IllegalMoveException.class)
    public ResponseEntity<Map<String, String>> handleIllegalMove(IllegalMoveException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleInvalidParameter(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Parâmetro inválido: " + e.getName()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(GameNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(GameOverException.class)
    public ResponseEntity<Map<String, String>> handleGameOver(GameOverException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
    }
}
