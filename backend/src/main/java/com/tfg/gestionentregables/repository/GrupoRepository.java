package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    
    List<Grupo> findByCursoId(Long cursoId);
}
