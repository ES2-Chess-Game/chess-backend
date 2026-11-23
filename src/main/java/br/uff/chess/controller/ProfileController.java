package br.uff.chess.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uff.chess.dto.ProfileDTO;
import br.uff.chess.dto.UpdateProfileRequest;
import br.uff.chess.service.ProfileService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/perfil")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileDTO consultar(Authentication auth) {
        return ProfileDTO.from(profileService.get((Long) auth.getPrincipal()));
    }

    @PutMapping
    public ProfileDTO editar(@Valid @RequestBody UpdateProfileRequest req, Authentication auth) {
        return ProfileDTO.from(profileService.update(
                (Long) auth.getPrincipal(), req.username(), req.email(), req.avatarId()));
    }
}
