package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Notificacion;
import com.tfg.gestionentregables.entity.enums.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    Long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    boolean existsByUsuarioIdAndTipoAndCursoIdAndTituloAndFechaCreacionAfter(
            Long usuarioId,
            TipoNotificacion tipo,
            Long cursoId,
            String titulo,
            LocalDateTime fecha);
}
