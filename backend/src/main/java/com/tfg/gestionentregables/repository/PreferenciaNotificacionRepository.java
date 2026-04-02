package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.PreferenciaNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreferenciaNotificacionRepository extends JpaRepository<PreferenciaNotificacion, Long> {

    Optional<PreferenciaNotificacion> findByUsuarioId(Long usuarioId);
}
