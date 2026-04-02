package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

            @Query("SELECT e FROM Estudiante e WHERE e.id = (" +
                "SELECT MIN(e2.id) FROM Estudiante e2 LEFT JOIN e2.grupo.cursos c2 " +
                "WHERE e2.usuario.id = :usuarioId AND (e2.grupo.curso.id = :cursoId OR c2.id = :cursoId)" +
                ")")
    Optional<Estudiante> findFirstByUsuarioIdAndGrupoCursoId(@Param("usuarioId") Long usuarioId,
                                                             @Param("cursoId") Long cursoId);

    boolean existsByUsuarioIdAndGrupoCursoId(Long usuarioId, Long cursoId);
}
