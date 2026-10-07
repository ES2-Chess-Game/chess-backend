package br.uff.chess.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.uff.chess.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
