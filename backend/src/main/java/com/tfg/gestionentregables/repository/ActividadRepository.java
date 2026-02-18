package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Actividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    
    List<Actividad> findByCursoId(Long cursoId);
    
    List<Actividad> findByCursoIdAndVisibilidad(Long cursoId, Visibilidad visibilidad);
    
    List<Actividad> findByCursoIdAndFechaLimiteAfter(Long cursoId, LocalDateTime fecha);
    
    List<Actividad> findByCursoIdAndFechaLimiteBetween(Long cursoId, LocalDateTime desde, LocalDateTime hasta);
    
    @Query("SELECT a FROM Actividad a JOIN a.grupos g WHERE g.id = :grupoId")
    List<Actividad> findByGrupoId(@Param("grupoId") Long grupoId);
    
    @Query("SELECT a FROM Actividad a JOIN a.grupos g WHERE g.id = :grupoId AND a.visibilidad = :visibilidad")
    List<Actividad> findByGrupoIdAndVisibilidad(@Param("grupoId") Long grupoId, @Param("visibilidad") Visibilidad visibilidad);
}
