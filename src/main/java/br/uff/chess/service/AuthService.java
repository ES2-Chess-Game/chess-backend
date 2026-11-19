package br.uff.chess.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.uff.chess.model.User;
import br.uff.chess.repository.UserRepository;
import br.uff.chess.service.exceptions.InvalidCredentialsException;
import br.uff.chess.service.exceptions.UserAlreadyExistsException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User register(String username, String email, String senha) {
        if (users.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Nome de usuário já cadastrado");
        }
        if (users.existsByEmail(email)) {
            throw new UserAlreadyExistsException("E-mail já cadastrado");
        }
        try {
            return users.saveAndFlush(new User(username, email, encoder.encode(senha)));
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Usuário ou e-mail já cadastrado");
        }
    }

    public User authenticate(String username, String senha) {
        User user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        if (!encoder.matches(senha, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
