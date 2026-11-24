package br.uff.chess.service;

import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.uff.chess.model.User;
import br.uff.chess.repository.UserRepository;
import br.uff.chess.repository.AvatarRepository;
import br.uff.chess.service.exceptions.AvatarNotFoundException;
import br.uff.chess.service.exceptions.InvalidCredentialsException;
import br.uff.chess.service.exceptions.PasswordTooLongException;
import br.uff.chess.service.exceptions.UserAlreadyExistsException;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AvatarRepository avatars;

    public AuthService(UserRepository users, PasswordEncoder encoder, AvatarRepository avatars) {
        this.users = users;
        this.encoder = encoder;
        this.avatars = avatars;
    }

    public User register(String username, String email, String senha, Long avatarId) {
        if (exceedsBcryptLimit(senha)) {
            throw new PasswordTooLongException();
        }
        if (users.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Nome de usuário já cadastrado");
        }
        if (users.existsByEmail(email)) {
            throw new UserAlreadyExistsException("E-mail já cadastrado");
        }
        var avatar = avatarId == null ? null
                : avatars.findById(avatarId).orElseThrow(() -> new AvatarNotFoundException(avatarId));
        User user = new User(username, email, encoder.encode(senha));
        user.setAvatar(avatar);
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Usuário ou e-mail já cadastrado");
        }
    }

    public User authenticate(String username, String senha) {
        User user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        if (exceedsBcryptLimit(senha) || !encoder.matches(senha, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    private boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
