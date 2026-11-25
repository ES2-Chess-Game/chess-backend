package br.uff.chess.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uff.chess.dto.AvatarDTO;
import br.uff.chess.model.GameResults;
import br.uff.chess.repository.GameStore;
import br.uff.chess.repository.UserRepository;

@RestController
public class RankingController {

    private final UserRepository users;
    private final GameStore games;

    public RankingController(UserRepository users, GameStore games) {
        this.users = users;
        this.games = games;
    }

    @GetMapping("/api/partidas/ranking")
    public List<Entry> ranking() {
        var results = games.rankingResults();
        var empty = new GameResults(0, 0, 0);
        return users.findAllWithAvatar().stream()
                .map(user -> {
                    GameResults stats = results.getOrDefault(user.getId(), empty);
                    AvatarDTO avatar = user.getAvatar() == null ? null : AvatarDTO.from(user.getAvatar());
                    return new Entry(user.getUsername(), avatar, stats.vitorias(), stats.derrotas(),
                            stats.empates(), stats.vitorias() * 3 + stats.empates());
                })
                .sorted(Comparator.comparingLong(Entry::pontos).reversed()
                        .thenComparingLong(Entry::derrotas))
                .toList();
    }

    public record Entry(String username, AvatarDTO avatar, long vitorias, long derrotas, long empates, long pontos) {
    }
}
