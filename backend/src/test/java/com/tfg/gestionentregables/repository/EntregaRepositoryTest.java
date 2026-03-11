package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EntregaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EntregaRepository entregaRepository;

    private Entregable entregable;
    private Estudiante estudiante1;
    private Estudiante estudiante2;
    private Actividad actividad;

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

        Usuario u1 = em.persistAndFlush(Usuario.builder()
                .nombre("Ana").correoElectronico("ana@test.com").contrasena("pass123").esAdmin(false).build());
        Usuario u2 = em.persistAndFlush(Usuario.builder()
                .nombre("Pedro").correoElectronico("pedro@test.com").contrasena("pass123").esAdmin(false).build());

        estudiante1 = em.persistAndFlush(Estudiante.builder().usuario(u1).grupo(grupo).build());
        estudiante2 = em.persistAndFlush(Estudiante.builder().usuario(u2).grupo(grupo).build());
    }

    private Entrega crearEntrega(Estudiante est, EstadoEntrega estado, boolean activa, int version) {
        return em.persistAndFlush(Entrega.builder()
                .nombre("Entrega " + est.getUsuario().getNombre())
                .version(version).fechaEntrega(LocalDateTime.now())
                .estado(estado).esVersionActiva(activa)
                .entregable(entregable).estudiante(est).build());
    }

    @Test
    @DisplayName("findByEntregableId devuelve todas las entregas del entregable")
    void findByEntregableId() {
        crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, true, 1);
        crearEntrega(estudiante2, EstadoEntrega.ENTREGADO, true, 1);

        List<Entrega> result = entregaRepository.findByEntregableId(entregable.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByEstudianteId devuelve entregas del estudiante")
    void findByEstudianteId() {
        crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, true, 1);
        crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, false, 2);
        crearEntrega(estudiante2, EstadoEntrega.ENTREGADO, true, 1);

        List<Entrega> result = entregaRepository.findByEstudianteId(estudiante1.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByEntregableIdAndEstudianteIdAndEsVersionActivaTrue devuelve versión activa")
    void findVersionActiva() {
        crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, false, 1);
        Entrega activa = crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, true, 2);

        Optional<Entrega> result = entregaRepository
                .findByEntregableIdAndEstudianteIdAndEsVersionActivaTrue(
                        entregable.getId(), estudiante1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(activa.getVersion());
    }

    @Test
    @DisplayName("findByEstadoAndEsVersionActiva filtra por estado y versión activa")
    void findByEstadoAndEsVersionActiva() {
        crearEntrega(estudiante1, EstadoEntrega.CALIFICADO, true, 1);
        crearEntrega(estudiante2, EstadoEntrega.ENTREGADO, true, 1);

        List<Entrega> calificados = entregaRepository.findByEstadoAndEsVersionActiva(
                EstadoEntrega.CALIFICADO, true);

        assertThat(calificados).hasSize(1);
    }

    @Test
    @DisplayName("findByActividadId devuelve entregas de la actividad via entregable")
    void findByActividadId() {
        crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, true, 1);

        List<Entrega> result = entregaRepository.findByActividadId(actividad.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByActividadIdAndEstado filtra por actividad y estado")
    void findByActividadIdAndEstado() {
        crearEntrega(estudiante1, EstadoEntrega.CALIFICADO, true, 1);
        crearEntrega(estudiante2, EstadoEntrega.ENTREGADO, true, 1);

        List<Entrega> result = entregaRepository.findByActividadIdAndEstado(
                actividad.getId(), EstadoEntrega.CALIFICADO);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("countEntregasByEntregableId cuenta solo versiones activas")
    void countEntregasByEntregableId() {
        crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, true, 2);
        crearEntrega(estudiante1, EstadoEntrega.ENTREGADO, false, 1);
        crearEntrega(estudiante2, EstadoEntrega.ENTREGADO, true, 1);

        Long count = entregaRepository.countEntregasByEntregableId(entregable.getId());

        assertThat(count).isEqualTo(2);
    }
}
