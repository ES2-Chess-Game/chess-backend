package br.uff.chess.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.uff.chess.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u left join fetch u.avatar")
    List<User> findAllWithAvatar();

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
