package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.OneDriveToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OneDriveTokenRepository extends JpaRepository<OneDriveToken, Long> {

    Optional<OneDriveToken> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
