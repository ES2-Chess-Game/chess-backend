package br.uff.chess.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uff.chess.dto.LoginRequest;
import br.uff.chess.dto.RegisterRequest;
import br.uff.chess.dto.UserDTO;
import br.uff.chess.model.User;
import br.uff.chess.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import br.uff.chess.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UserDTO> cadastro(@Valid @RequestBody RegisterRequest req) {
        User user = authService.register(req.username(), req.email(), req.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.from(user));
    }

    @PostMapping("/login")
    public UserDTO login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        User user = authService.authenticate(req.username(), req.senha());

        // Troca o id da sessão existente contra session fixation
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                user.getId(), null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return UserDTO.from(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(Authentication auth) {
        return users.findById((Long) auth.getPrincipal())
                .map(user -> ResponseEntity.ok(UserDTO.from(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
