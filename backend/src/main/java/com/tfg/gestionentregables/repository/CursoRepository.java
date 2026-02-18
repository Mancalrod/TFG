package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    
    Optional<Curso> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    @Query("SELECT c FROM Curso c JOIN c.profesores p WHERE p.usuario.id = :usuarioId")
    List<Curso> findByProfesorUsuarioId(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT DISTINCT c FROM Curso c JOIN c.grupos g JOIN g.estudiantes e WHERE e.usuario.id = :usuarioId")
    List<Curso> findByEstudianteUsuarioId(@Param("usuarioId") Long usuarioId);
}
