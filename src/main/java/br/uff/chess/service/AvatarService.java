package br.uff.chess.service;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import br.uff.chess.model.Avatar;
import br.uff.chess.repository.AvatarRepository;

/** Lista o catálogo e faz a carga inicial quando o banco está vazio. */
@Service
public class AvatarService implements ApplicationRunner {

    private static final List<Avatar> INITIAL_CATALOG = List.of(
            new Avatar("Rei", "king.svg"),
            new Avatar("Rainha", "queen.svg"),
            new Avatar("Torre", "rook.svg"),
            new Avatar("Bispo", "bishop.svg"),
            new Avatar("Cavalo", "knight.svg"),
            new Avatar("Peão", "pawn.svg"));

    private final AvatarRepository avatarRepository;

    public AvatarService(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    public List<Avatar> listAll() {
        return avatarRepository.findAll();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (avatarRepository.count() == 0) {
            avatarRepository.saveAll(INITIAL_CATALOG);
        }
    }
}
