package br.uff.chess.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uff.chess.model.Avatar;
import br.uff.chess.model.User;
import br.uff.chess.repository.AvatarRepository;
import br.uff.chess.repository.UserRepository;
import br.uff.chess.service.exceptions.AvatarNotFoundException;
import br.uff.chess.service.exceptions.InvalidCredentialsException;
import br.uff.chess.service.exceptions.UserAlreadyExistsException;

@Service
public class ProfileService {

    private final UserRepository users;
    private final AvatarRepository avatars;

    public ProfileService(UserRepository users, AvatarRepository avatars) {
        this.users = users;
        this.avatars = avatars;
    }

    public User get(Long userId) {
        return users.findById(userId).orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional
    public User update(Long userId, String username, String email, Long avatarId) {
        User user = get(userId);
        Avatar avatar = avatarId == null ? null
                : avatars.findById(avatarId).orElseThrow(() -> new AvatarNotFoundException(avatarId));

        if (username != null && !username.equals(user.getUsername())) {
            if (users.existsByUsername(username)) {
                throw new UserAlreadyExistsException("Nome de usuário já cadastrado");
            }
            user.setUsername(username);
        }
        if (email != null && !email.equals(user.getEmail())) {
            if (users.existsByEmail(email)) {
                throw new UserAlreadyExistsException("E-mail já cadastrado");
            }
            user.setEmail(email);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Usuário ou e-mail já cadastrado");
        }
    }
}
