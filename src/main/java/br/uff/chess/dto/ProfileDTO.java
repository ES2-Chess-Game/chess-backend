package br.uff.chess.dto;

import br.uff.chess.model.User;
import br.uff.chess.model.UserStats;

public record ProfileDTO(Long id, String username, String email, AvatarDTO avatar, StatsDTO estatisticas) {

    public record StatsDTO(int vitorias, int derrotas, int empates) {

        static StatsDTO from(UserStats stats) {
            return new StatsDTO(stats.getWins(), stats.getLosses(), stats.getDraws());
        }
    }

    public static ProfileDTO from(User user) {
        AvatarDTO avatar = user.getAvatar() == null ? null : AvatarDTO.from(user.getAvatar());
        return new ProfileDTO(user.getId(), user.getUsername(), user.getEmail(), avatar,
                StatsDTO.from(user.getStats()));
    }
}
