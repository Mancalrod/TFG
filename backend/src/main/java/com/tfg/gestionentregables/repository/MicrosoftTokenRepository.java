package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.MicrosoftToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MicrosoftTokenRepository extends JpaRepository<MicrosoftToken, Long> {

    Optional<MicrosoftToken> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
