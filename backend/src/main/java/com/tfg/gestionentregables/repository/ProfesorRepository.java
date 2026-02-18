package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    
    Optional<Profesor> findFirstByUsuarioId(Long usuarioId);
    
    List<Profesor> findByUsuarioId(Long usuarioId);
    
    List<Profesor> findByCursoId(Long cursoId);
    
    Optional<Profesor> findByUsuarioIdAndCursoId(Long usuarioId, Long cursoId);
    
    boolean existsByUsuarioId(Long usuarioId);
    
    boolean existsByUsuarioIdAndCursoId(Long usuarioId, Long cursoId);
}
