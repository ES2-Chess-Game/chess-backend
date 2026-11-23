package br.uff.chess.dto;

import br.uff.chess.model.GameResults;
import br.uff.chess.model.User;

public record ProfileDTO(Long id, String username, String email, AvatarDTO avatar, GameResults estatisticas) {

    public static ProfileDTO from(User user, GameResults results) {
        AvatarDTO avatar = user.getAvatar() == null ? null : AvatarDTO.from(user.getAvatar());
        return new ProfileDTO(user.getId(), user.getUsername(), user.getEmail(), avatar, results);
    }
}
