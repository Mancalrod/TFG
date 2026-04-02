package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    
    Optional<Estudiante> findFirstByUsuarioId(Long usuarioId);
    
    List<Estudiante> findByUsuarioId(Long usuarioId);
    
    List<Estudiante> findByGrupoId(Long grupoId);
    
    Optional<Estudiante> findByUsuarioIdAndGrupoId(Long usuarioId, Long grupoId);
    
    boolean existsByUsuarioId(Long usuarioId);
    
    boolean existsByUsuarioIdAndGrupoId(Long usuarioId, Long grupoId);

    Optional<Estudiante> findFirstByUsuarioIdAndGrupoCursoId(Long usuarioId, Long cursoId);

    boolean existsByUsuarioIdAndGrupoCursoId(Long usuarioId, Long cursoId);
}
