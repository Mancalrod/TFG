package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    List<Feedback> findByEntregaId(Long entregaId);
    
    List<Feedback> findByEntregaIdOrderByFechaCreacionDesc(Long entregaId);
    
    List<Feedback> findByProfesorId(Long profesorId);
    
    List<Feedback> findByProfesorIdOrderByFechaCreacionDesc(Long profesorId);
    
    @Query("SELECT COUNT(f) FROM Feedback f JOIN f.entrega e WHERE e.estudiante.id = :estudianteId AND f.fechaCreacion > :desde")
    long countFeedbacksRecientesParaEstudiante(@Param("estudianteId") Long estudianteId, @Param("desde") LocalDateTime desde);
}
