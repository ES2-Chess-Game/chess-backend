package br.uff.chess.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uff.chess.dto.AvatarDTO;
import br.uff.chess.service.AvatarService;

@RestController
@RequestMapping("/api/avatares")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @GetMapping
    public List<AvatarDTO> listar() {
        return avatarService.listAll().stream().map(AvatarDTO::from).toList();
    }
}
