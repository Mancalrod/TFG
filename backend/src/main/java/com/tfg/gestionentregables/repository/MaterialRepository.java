package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    
    List<Material> findByActividadId(Long actividadId);
    
    List<Material> findByEntregableId(Long entregableId);
    
    List<Material> findByEntregaId(Long entregaId);
}
