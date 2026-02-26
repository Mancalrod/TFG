package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class EntityMapperTest {

    private EntityMapper mapper;

    // Entidades compartidas
    private Usuario usuario;
    private Curso curso;
    private Grupo grupo;
    private Actividad actividad;
    private Entregable entregable;
    private Estudiante estudiante;
    private Entrega entrega;
    private Material material;
    private Feedback feedback;

    @BeforeEach
    void setUp() {
        mapper = new EntityMapper();

        usuario = Usuario.builder()
                .id(1L).nombre("Juan García").telefono("600123456")
                .correoElectronico("juan@test.com").contrasena("pass123")
                .esAdmin(false).build();

        curso = Curso.builder()
                .id(1L).titulo("Ingeniería del Software").descripcion("Curso IS")
                .codigo("IS-001")
                .profesores(new HashSet<>()).grupos(new HashSet<>())
                .actividades(new HashSet<>()).build();

        grupo = Grupo.builder()
                .id(1L).titulo("Grupo A").curso(curso)
                .estudiantes(new HashSet<>()).build();

        curso.getGrupos().add(grupo);

        actividad = Actividad.builder()
                .id(1L).titulo("Práctica 1").descripcion("Primera práctica")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(LocalDateTime.of(2026, 2, 1, 10, 0))
                .fechaInicio(LocalDateTime.of(2026, 2, 1, 10, 0))
                .fechaLimite(LocalDateTime.of(2030, 12, 31, 23, 59))
                .visibilidad(Visibilidad.VISIBLE).notaMaxima(10.0)
                .curso(curso).grupos(new HashSet<>(Set.of(grupo)))
                .entregables(new HashSet<>()).materiales(new HashSet<>())
                .build();

        entregable = Entregable.builder()
                .id(1L).titulo("Entregable 1").descripcion("Desc entregable")
                .fechaInicio(LocalDateTime.of(2026, 2, 1, 10, 0))
                .fechaLimite(LocalDateTime.of(2030, 12, 31, 23, 59))
                .notaMaxima(10.0).tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(5242880L).visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true).actividad(actividad)
                .entregas(new HashSet<>()).materiales(new HashSet<>())
                .build();

        estudiante = Estudiante.builder()
                .id(1L).usuario(usuario).grupo(grupo).build();

        entrega = Entrega.builder()
                .id(1L).nombre("Mi entrega").version(1)
                .fechaEntrega(LocalDateTime.of(2026, 3, 1, 12, 0))
                .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                .calificacion(null).fechaCalificacion(null)
                .entregable(entregable).estudiante(estudiante)
                .archivos(new HashSet<>()).feedbacks(new HashSet<>())
                .build();

        material = Material.builder()
                .id(1L).nombre("archivo.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/archivo.pdf").tamanoBytes(1024L)
                .build();

        feedback = Feedback.builder()
                .id(1L).comentario("Buen trabajo")
                .fechaCreacion(LocalDateTime.of(2026, 3, 5, 10, 0))
                .fechaModificacion(LocalDateTime.of(2026, 3, 5, 11, 0))
                .entrega(entrega).profesor(usuario)
                .build();
    }

    // ==================== UsuarioDTO ====================

    @Nested
    @DisplayName("toDTO(Usuario)")
    class UsuarioMapping {

        @Test
        @DisplayName("Mapea usuario correctamente")
        void mapea_ok() {
            UsuarioDTO dto = mapper.toDTO(usuario);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getNombre()).isEqualTo("Juan García");
            assertThat(dto.getTelefono()).isEqualTo("600123456");
            assertThat(dto.getCorreoElectronico()).isEqualTo("juan@test.com");
            assertThat(dto.getEsAdmin()).isFalse();
        }

        @Test
        @DisplayName("Devuelve null si usuario es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Usuario) null)).isNull();
        }

        @Test
        @DisplayName("Mapea usuario admin")
        void mapea_admin() {
            usuario.setEsAdmin(true);
            UsuarioDTO dto = mapper.toDTO(usuario);

            assertThat(dto.getEsAdmin()).isTrue();
        }
    }

    // ==================== CursoDTO ====================

    @Nested
    @DisplayName("toDTO(Curso)")
    class CursoMapping {

        @Test
        @DisplayName("Mapea curso correctamente con grupos y contadores")
        void mapea_ok() {
            curso.getActividades().add(actividad);
            Profesor prof = Profesor.builder().id(1L).usuario(usuario).curso(curso).build();
            curso.getProfesores().add(prof);

            Estudiante est1 = Estudiante.builder().id(1L).usuario(usuario).grupo(grupo).build();
            grupo.getEstudiantes().add(est1);

            CursoDTO dto = mapper.toDTO(curso);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitulo()).isEqualTo("Ingeniería del Software");
            assertThat(dto.getDescripcion()).isEqualTo("Curso IS");
            assertThat(dto.getCodigo()).isEqualTo("IS-001");
            assertThat(dto.getGrupos()).hasSize(1);
            assertThat(dto.getNumeroActividades()).isEqualTo(1);
            assertThat(dto.getNumeroProfesores()).isEqualTo(1);
            assertThat(dto.getNumeroEstudiantes()).isEqualTo(1);
        }

        @Test
        @DisplayName("Devuelve null si curso es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Curso) null)).isNull();
        }

        @Test
        @DisplayName("Mapea curso sin datos (contadores a 0)")
        void mapea_vacio() {
            CursoDTO dto = mapper.toDTO(curso);

            assertThat(dto.getNumeroActividades()).isZero();
            assertThat(dto.getNumeroProfesores()).isZero();
            assertThat(dto.getNumeroEstudiantes()).isZero();
            assertThat(dto.getGrupos()).hasSize(1); // tiene 1 grupo pero sin estudiantes
        }
    }

    // ==================== GrupoDTO ====================

    @Nested
    @DisplayName("toDTO(Grupo)")
    class GrupoMapping {

        @Test
        @DisplayName("Mapea grupo correctamente")
        void mapea_ok() {
            GrupoDTO dto = mapper.toDTO(grupo);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitulo()).isEqualTo("Grupo A");
            assertThat(dto.getCursoId()).isEqualTo(1L);
            assertThat(dto.getCursoTitulo()).isEqualTo("Ingeniería del Software");
            assertThat(dto.getNumeroEstudiantes()).isZero();
        }

        @Test
        @DisplayName("Devuelve null si grupo es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Grupo) null)).isNull();
        }

        @Test
        @DisplayName("Mapea grupo con estudiantes")
        void mapea_conEstudiantes() {
            grupo.getEstudiantes().add(estudiante);

            GrupoDTO dto = mapper.toDTO(grupo);

            assertThat(dto.getNumeroEstudiantes()).isEqualTo(1);
        }
    }

    // ==================== GrupoDTO (simple) ====================

    @Nested
    @DisplayName("toSimpleDTO(Grupo)")
    class GrupoSimpleMapping {

        @Test
        @DisplayName("Mapea grupo simple sin cursoId ni cursoTitulo")
        void mapea_ok() {
            GrupoDTO dto = mapper.toSimpleDTO(grupo);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitulo()).isEqualTo("Grupo A");
            assertThat(dto.getCursoId()).isNull();
            assertThat(dto.getCursoTitulo()).isNull();
            assertThat(dto.getNumeroEstudiantes()).isZero();
        }

        @Test
        @DisplayName("Devuelve null si grupo es null")
        void mapea_null() {
            assertThat(mapper.toSimpleDTO(null)).isNull();
        }
    }

    // ==================== ActividadDTO ====================

    @Nested
    @DisplayName("toDTO(Actividad)")
    class ActividadMapping {

        @Test
        @DisplayName("Mapea actividad correctamente")
        void mapea_ok() {
            ActividadDTO dto = mapper.toDTO(actividad);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitulo()).isEqualTo("Práctica 1");
            assertThat(dto.getDescripcion()).isEqualTo("Primera práctica");
            assertThat(dto.getTipoActividad()).isEqualTo(TipoActividad.EVALUABLE);
            assertThat(dto.getVisibilidad()).isEqualTo(Visibilidad.VISIBLE);
            assertThat(dto.getNotaMaxima()).isEqualTo(10.0);
            assertThat(dto.getCursoId()).isEqualTo(1L);
            assertThat(dto.getCursoTitulo()).isEqualTo("Ingeniería del Software");
            assertThat(dto.getGrupoIds()).containsExactly(1L);
            assertThat(dto.getNumeroEntregables()).isZero();
            assertThat(dto.getEnPlazo()).isTrue();
        }

        @Test
        @DisplayName("Devuelve null si actividad es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Actividad) null)).isNull();
        }

        @Test
        @DisplayName("Mapea actividad con entregables contados")
        void mapea_conEntregables() {
            actividad.getEntregables().add(entregable);
            ActividadDTO dto = mapper.toDTO(actividad);

            assertThat(dto.getNumeroEntregables()).isEqualTo(1);
        }

        @Test
        @DisplayName("Mapea actividad fuera de plazo")
        void mapea_fueraDePlazo() {
            actividad.setFechaLimite(LocalDateTime.of(2020, 1, 1, 0, 0));
            ActividadDTO dto = mapper.toDTO(actividad);

            assertThat(dto.getEnPlazo()).isFalse();
        }
    }

    // ==================== ActividadDTO con Entregables ====================

    @Nested
    @DisplayName("toDTOWithEntregables(Actividad)")
    class ActividadConEntregablesMapping {

        @Test
        @DisplayName("Mapea actividad con lista de entregables")
        void mapea_ok() {
            actividad.getEntregables().add(entregable);

            ActividadDTO dto = mapper.toDTOWithEntregables(actividad);

            assertThat(dto.getEntregables()).hasSize(1);
            assertThat(dto.getEntregables().get(0).getTitulo()).isEqualTo("Entregable 1");
        }

        @Test
        @DisplayName("Mapea actividad sin entregables → lista vacía")
        void mapea_sinEntregables() {
            ActividadDTO dto = mapper.toDTOWithEntregables(actividad);

            assertThat(dto.getEntregables()).isEmpty();
        }

        @Test
        @DisplayName("Devuelve null si actividad es null")
        void mapea_null() {
            assertThat(mapper.toDTOWithEntregables(null)).isNull();
        }
    }

    // ==================== EntregableDTO ====================

    @Nested
    @DisplayName("toDTO(Entregable)")
    class EntregableMapping {

        @Test
        @DisplayName("Mapea entregable correctamente")
        void mapea_ok() {
            EntregableDTO dto = mapper.toDTO(entregable);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitulo()).isEqualTo("Entregable 1");
            assertThat(dto.getDescripcion()).isEqualTo("Desc entregable");
            assertThat(dto.getNotaMaxima()).isEqualTo(10.0);
            assertThat(dto.getTipoArchivoEsperado()).isEqualTo(TipoMaterial.PDF);
            assertThat(dto.getTamanoMaximoBytes()).isEqualTo(5242880L);
            assertThat(dto.getVisibilidad()).isEqualTo(Visibilidad.VISIBLE);
            assertThat(dto.getPermiteReenvio()).isTrue();
            assertThat(dto.getActividadId()).isEqualTo(1L);
            assertThat(dto.getActividadTitulo()).isEqualTo("Práctica 1");
            assertThat(dto.getNumeroEntregas()).isZero();
            assertThat(dto.getEnPlazo()).isTrue();
        }

        @Test
        @DisplayName("Devuelve null si entregable es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Entregable) null)).isNull();
        }

        @Test
        @DisplayName("Cuenta entregas correctamente")
        void mapea_conEntregas() {
            entregable.getEntregas().add(entrega);

            EntregableDTO dto = mapper.toDTO(entregable);

            assertThat(dto.getNumeroEntregas()).isEqualTo(1L);
        }
    }

    // ==================== EntregaDTO ====================

    @Nested
    @DisplayName("toDTO(Entrega)")
    class EntregaMapping {

        @Test
        @DisplayName("Mapea entrega correctamente con archivos y feedbacks vacíos")
        void mapea_ok() {
            EntregaDTO dto = mapper.toDTO(entrega);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getNombre()).isEqualTo("Mi entrega");
            assertThat(dto.getVersion()).isEqualTo(1);
            assertThat(dto.getEstado()).isEqualTo(EstadoEntrega.ENTREGADO);
            assertThat(dto.getEsVersionActiva()).isTrue();
            assertThat(dto.getCalificacion()).isNull();
            assertThat(dto.getEntregableId()).isEqualTo(1L);
            assertThat(dto.getEntregableTitulo()).isEqualTo("Entregable 1");
            assertThat(dto.getEstudianteId()).isEqualTo(1L);
            assertThat(dto.getEstudianteNombre()).isEqualTo("Juan García");
            assertThat(dto.getFueATiempo()).isTrue();
            assertThat(dto.getArchivos()).isEmpty();
            assertThat(dto.getFeedbacks()).isEmpty();
        }

        @Test
        @DisplayName("Devuelve null si entrega es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Entrega) null)).isNull();
        }

        @Test
        @DisplayName("Mapea entrega con archivos y feedbacks")
        void mapea_conArchivosYFeedbacks() {
            material.setEntrega(entrega);
            entrega.getArchivos().add(material);
            entrega.getFeedbacks().add(feedback);

            EntregaDTO dto = mapper.toDTO(entrega);

            assertThat(dto.getArchivos()).hasSize(1);
            assertThat(dto.getFeedbacks()).hasSize(1);
        }

        @Test
        @DisplayName("Mapea entrega calificada")
        void mapea_calificada() {
            entrega.setCalificacion(8.5);
            entrega.setEstado(EstadoEntrega.CALIFICADO);
            entrega.setFechaCalificacion(LocalDateTime.of(2026, 3, 10, 14, 0));

            EntregaDTO dto = mapper.toDTO(entrega);

            assertThat(dto.getCalificacion()).isEqualTo(8.5);
            assertThat(dto.getEstado()).isEqualTo(EstadoEntrega.CALIFICADO);
            assertThat(dto.getFechaCalificacion()).isNotNull();
        }

        @Test
        @DisplayName("Mapea entrega con archivos null → lista vacía")
        void mapea_archivosNull() {
            entrega.setArchivos(null);
            entrega.setFeedbacks(null);

            EntregaDTO dto = mapper.toDTO(entrega);

            assertThat(dto.getArchivos()).isEmpty();
            assertThat(dto.getFeedbacks()).isEmpty();
        }
    }

    // ==================== EntregaResumenDTO ====================

    @Nested
    @DisplayName("toResumenDTO(Entrega)")
    class EntregaResumenMapping {

        @Test
        @DisplayName("Mapea resumen de entrega correctamente")
        void mapea_ok() {
            EntregaResumenDTO dto = mapper.toResumenDTO(entrega);

            assertThat(dto.getEntregaId()).isEqualTo(1L);
            assertThat(dto.getEstudianteId()).isEqualTo(1L);
            assertThat(dto.getEstudianteNombre()).isEqualTo("Juan García");
            assertThat(dto.getEstudianteCorreo()).isEqualTo("juan@test.com");
            assertThat(dto.getGrupoTitulo()).isEqualTo("Grupo A");
            assertThat(dto.getEstado()).isEqualTo(EstadoEntrega.ENTREGADO);
            assertThat(dto.getCalificacion()).isNull();
            assertThat(dto.getFueATiempo()).isTrue();
            assertThat(dto.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Devuelve null si entrega es null")
        void mapea_null() {
            assertThat(mapper.toResumenDTO(null)).isNull();
        }
    }

    // ==================== MaterialDTO ====================

    @Nested
    @DisplayName("toDTO(Material)")
    class MaterialMapping {

        @Test
        @DisplayName("Mapea material correctamente")
        void mapea_ok() {
            MaterialDTO dto = mapper.toDTO(material);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getNombre()).isEqualTo("archivo.pdf");
            assertThat(dto.getTipoMaterial()).isEqualTo(TipoMaterial.PDF);
            assertThat(dto.getRuta()).isEqualTo("/uploads/archivo.pdf");
            assertThat(dto.getTamanoBytes()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("Devuelve null si material es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Material) null)).isNull();
        }
    }

    // ==================== FeedbackDTO ====================

    @Nested
    @DisplayName("toDTO(Feedback)")
    class FeedbackMapping {

        @Test
        @DisplayName("Mapea feedback correctamente")
        void mapea_ok() {
            FeedbackDTO dto = mapper.toDTO(feedback);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getComentario()).isEqualTo("Buen trabajo");
            assertThat(dto.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 3, 5, 10, 0));
            assertThat(dto.getFechaModificacion()).isEqualTo(LocalDateTime.of(2026, 3, 5, 11, 0));
            assertThat(dto.getEntregaId()).isEqualTo(1L);
            assertThat(dto.getProfesorId()).isEqualTo(1L);
            assertThat(dto.getProfesorNombre()).isEqualTo("Juan García");
        }

        @Test
        @DisplayName("Devuelve null si feedback es null")
        void mapea_null() {
            assertThat(mapper.toDTO((Feedback) null)).isNull();
        }

        @Test
        @DisplayName("Mapea feedback sin fecha de modificación")
        void mapea_sinModificacion() {
            feedback.setFechaModificacion(null);

            FeedbackDTO dto = mapper.toDTO(feedback);

            assertThat(dto.getFechaModificacion()).isNull();
        }
    }
}
