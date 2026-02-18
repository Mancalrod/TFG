package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Entregable;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EntregableRepository extends JpaRepository<Entregable, Long> {
    
    List<Entregable> findByActividadId(Long actividadId);
    
    List<Entregable> findByActividadIdAndVisibilidad(Long actividadId, Visibilidad visibilidad);
    
    List<Entregable> findByActividadIdAndFechaLimiteAfter(Long actividadId, LocalDateTime fecha);
    
    List<Entregable> findByActividadIdAndFechaLimiteBetween(Long actividadId, LocalDateTime desde, LocalDateTime hasta);
}
