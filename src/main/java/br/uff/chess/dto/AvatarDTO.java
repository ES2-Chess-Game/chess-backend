package br.uff.chess.dto;

import br.uff.chess.model.Avatar;

public record AvatarDTO(Long id, String name, String image) {

    public static AvatarDTO from(Avatar avatar) {
        return new AvatarDTO(avatar.getId(), avatar.getName(), avatar.getImage());
    }
}
