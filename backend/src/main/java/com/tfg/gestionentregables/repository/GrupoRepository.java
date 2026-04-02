package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    List<Grupo> findByCursoId(Long cursoId);

    @Query("SELECT DISTINCT g FROM Grupo g LEFT JOIN g.cursos c WHERE g.curso.id = :cursoId OR c.id = :cursoId")
    List<Grupo> findByCursoRelacionadoId(@Param("cursoId") Long cursoId);

    @Query("SELECT DISTINCT g FROM Grupo g LEFT JOIN FETCH g.cursos")
    List<Grupo> findAllWithCursos();
}
