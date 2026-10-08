package br.uff.chess.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Campos nulos não são alterados. */
public record UpdateProfileRequest(
        @Size(min = 3, max = 30) String username,
        @Email String email,
        Long avatarId) {
}
