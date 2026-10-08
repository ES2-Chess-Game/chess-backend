package br.uff.chess.dto;

import br.uff.chess.model.User;

public record UserDTO(Long id, String username, String email, AvatarDTO avatar) {

    public static UserDTO from(User user) {
        AvatarDTO avatar = user.getAvatar() == null ? null : AvatarDTO.from(user.getAvatar());
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail(), avatar);
    }
}
