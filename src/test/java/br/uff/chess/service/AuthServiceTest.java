package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.uff.chess.model.User;
import br.uff.chess.repository.UserRepository;
import br.uff.chess.service.exceptions.InvalidCredentialsException;
import br.uff.chess.service.exceptions.UserAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AuthService service() {
        return new AuthService(users, encoder);
    }

    @Test
    void cadastroGuardaSenhaComHash() {
        when(users.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service().register("ana", "ana@email.com", "segredo123");

        assertNotEquals("segredo123", user.getPasswordHash());
        assertTrue(encoder.matches("segredo123", user.getPasswordHash()));
    }

    @Test
    void cadastroRecusaNomeDeUsuarioRepetido() {
        when(users.existsByUsername("ana")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> service().register("ana", "nova@email.com", "x"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void cadastroRecusaEmailRepetido() {
        when(users.existsByEmail("ana@email.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> service().register("nova", "ana@email.com", "x"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void cadastroSimultaneoQueViolaUnicidadeViraUsuarioJaExistente() {
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThrows(UserAlreadyExistsException.class, () -> service().register("ana", "ana@email.com", "x"));
    }

    @Test
    void loginComSenhaCorretaDevolveOUsuario() {
        User ana = new User("ana", "ana@email.com", encoder.encode("segredo123"));
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana));

        assertSame(ana, service().authenticate("ana", "segredo123"));
    }

    @Test
    void loginComSenhaErradaFalha() {
        User ana = new User("ana", "ana@email.com", encoder.encode("segredo123"));
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana));

        assertThrows(InvalidCredentialsException.class, () -> service().authenticate("ana", "errada"));
    }

    @Test
    void loginDeUsuarioInexistenteFalha() {
        when(users.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service().authenticate("ninguem", "x"));
    }
}
