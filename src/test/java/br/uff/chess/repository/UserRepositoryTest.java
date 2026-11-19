package br.uff.chess.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.uff.chess.model.Avatar;
import br.uff.chess.model.User;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    AvatarRepository avatarRepository;

    @Test
    void persisteUsuarioComEstatisticasEAvatar() {
        Avatar avatar = avatarRepository.findAll().get(0);
        User user = new User("ana", "ana@x.com", "hash");
        user.setAvatar(avatar);
        user.getStats().addWin();
        user.getStats().addDraw();
        userRepository.saveAndFlush(user);

        User loaded = userRepository.findByUsername("ana").orElseThrow();
        assertEquals(1, loaded.getStats().getWins());
        assertEquals(0, loaded.getStats().getLosses());
        assertEquals(1, loaded.getStats().getDraws());
        assertEquals(avatar.getId(), loaded.getAvatar().getId());
        assertTrue(userRepository.existsByEmail("ana@x.com"));
    }
}
