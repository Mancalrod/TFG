package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Entrega;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    
    List<Entrega> findByEntregableId(Long entregableId);
    
    List<Entrega> findByEstudianteId(Long estudianteId);
    
    List<Entrega> findByEntregableIdAndEstudianteId(Long entregableId, Long estudianteId);
    
    Optional<Entrega> findByEntregableIdAndEstudianteIdAndEsVersionActivaTrue(Long entregableId, Long estudianteId);
    
    List<Entrega> findByEntregableIdAndEsVersionActiva(Long entregableId, Boolean esVersionActiva);
    
    List<Entrega> findByEstadoAndEsVersionActiva(EstadoEntrega estado, Boolean esVersionActiva);
    
    @Query("SELECT e FROM Entrega e WHERE e.entregable.actividad.id = :actividadId")
    List<Entrega> findByActividadId(@Param("actividadId") Long actividadId);
    
    @Query("SELECT e FROM Entrega e WHERE e.entregable.actividad.id = :actividadId AND e.estado = :estado")
    List<Entrega> findByActividadIdAndEstado(@Param("actividadId") Long actividadId, @Param("estado") EstadoEntrega estado);
    
    @Query("SELECT COUNT(e) FROM Entrega e WHERE e.entregable.id = :entregableId AND e.esVersionActiva = true")
    Long countEntregasByEntregableId(@Param("entregableId") Long entregableId);
}
