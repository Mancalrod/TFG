package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MaterialRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MaterialRepository materialRepository;

    private Actividad actividad;
    private Entregable entregable;
    private Entrega entrega;

    @BeforeEach
    void setUp() {
        Curso curso = em.persistAndFlush(Curso.builder()
                .titulo("Curso").descripcion("Desc").codigo("C-001").build());
        Grupo grupo = em.persistAndFlush(Grupo.builder().titulo("Grupo A").curso(curso).build());

        actividad = em.persistAndFlush(Actividad.builder()
                .titulo("Actividad 1").tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(LocalDateTime.now()).fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE).curso(curso).build());

        entregable = em.persistAndFlush(Entregable.builder()
                .titulo("Entregable 1").fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE).actividad(actividad).build());

        Usuario alumno = em.persistAndFlush(Usuario.builder()
                .nombre("Ana").correoElectronico("ana@test.com").contrasena("pass123").esAdmin(false).build());
        Estudiante estudiante = em.persistAndFlush(Estudiante.builder().usuario(alumno).grupo(grupo).build());

        entrega = em.persistAndFlush(Entrega.builder()
                .nombre("Entrega 1").version(1).fechaEntrega(LocalDateTime.now())
                .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                .entregable(entregable).estudiante(estudiante).build());
    }

    @Test
    @DisplayName("findByActividadId devuelve materiales de la actividad")
    void findByActividadId() {
        em.persistAndFlush(Material.builder()
                .nombre("guia.pdf").tipoMaterial(TipoMaterial.PDF)
                .ruta("/mat/guia.pdf").tamanoBytes(1024L).actividad(actividad).build());

        List<Material> result = materialRepository.findByActividadId(actividad.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("guia.pdf");
    }

    @Test
    @DisplayName("findByEntregableId devuelve materiales del entregable")
    void findByEntregableId() {
        em.persistAndFlush(Material.builder()
                .nombre("rubrica.pdf").tipoMaterial(TipoMaterial.PDF)
                .ruta("/mat/rubrica.pdf").tamanoBytes(500L).entregable(entregable).build());

        List<Material> result = materialRepository.findByEntregableId(entregable.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByEntregaId devuelve archivos de la entrega")
    void findByEntregaId() {
        em.persistAndFlush(Material.builder()
                .nombre("entrega.pdf").tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entrega.pdf").tamanoBytes(2048L).entrega(entrega).build());

        List<Material> result = materialRepository.findByEntregaId(entrega.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("entrega.pdf");
    }
}
