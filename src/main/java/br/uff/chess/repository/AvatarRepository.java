package br.uff.chess.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.uff.chess.model.Avatar;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {
}
