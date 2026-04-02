package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActividadServiceTest {

    @Mock private ActividadRepository actividadRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private EntityMapper mapper;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private ActividadService actividadService;

    private Curso curso;
    private Grupo grupo;
    private Actividad actividad;
    private ActividadDTO actividadDTO;
    private CrearActividadDTO crearActividadDTO;

    @BeforeEach
    void setUp() {
        curso = Curso.builder()
                .id(1L)
                .titulo("Ingeniería del Software")
                .codigo("IS-001")
                .build();

        grupo = Grupo.builder()
            .id(1L)
            .titulo("G1")
            .curso(curso)
            .build();

        actividad = Actividad.builder()
                .id(1L)
                .titulo("Práctica 1")
                .descripcion("Descripción")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(LocalDateTime.now())
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .curso(curso)
                .build();

        actividadDTO = ActividadDTO.builder()
                .id(1L)
                .titulo("Práctica 1")
                .descripcion("Descripción")
                .tipoActividad(TipoActividad.EVALUABLE)
                .visibilidad(Visibilidad.VISIBLE)
                .cursoId(1L)
                .cursoTitulo("Ingeniería del Software")
                .grupoIds(List.of())
                .numeroEntregables(0)
                .enPlazo(true)
                .build();

        crearActividadDTO = CrearActividadDTO.builder()
                .titulo("Práctica 1")
                .descripcion("Descripción")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .build();
    }

    @Nested
    @DisplayName("crearActividad")
    class CrearActividad {

        @Test
        @DisplayName("Crea actividad correctamente")
        void crearActividad_ok() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(actividadRepository.save(any(Actividad.class))).thenReturn(actividad);
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            ActividadDTO result = actividadService.crearActividad(crearActividadDTO, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getTitulo()).isEqualTo("Práctica 1");
            verify(actividadRepository).save(any(Actividad.class));
        }

        @Test
        @DisplayName("Crea actividad con grupos específicos")
        void crearActividad_conGrupos() {
            crearActividadDTO.setGrupoIds(List.of(1L, 2L));
            Grupo g1 = Grupo.builder().id(1L).titulo("G1").curso(curso).build();
            Grupo g2 = Grupo.builder().id(2L).titulo("G2").curso(curso).build();

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(grupoRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(g1, g2));
            when(actividadRepository.save(any(Actividad.class))).thenReturn(actividad);
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            ActividadDTO result = actividadService.crearActividad(crearActividadDTO, 1L);

            assertThat(result).isNotNull();
            verify(grupoRepository).findAllById(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Lanza excepción si curso no existe")
        void crearActividad_cursoNoExiste() {
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> actividadService.crearActividad(crearActividadDTO, 99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Curso no encontrado");
        }

        @Test
        @DisplayName("Usa visibilidad OCULTO por defecto si no se especifica")
        void crearActividad_visibilidadPorDefecto() {
            crearActividadDTO.setVisibilidad(null);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> {
                Actividad a = inv.getArgument(0);
                assertThat(a.getVisibilidad()).isEqualTo(Visibilidad.OCULTO);
                a.setId(1L);
                return a;
            });
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            actividadService.crearActividad(crearActividadDTO, 1L);

            verify(actividadRepository).save(any(Actividad.class));
        }

        @Test
        @DisplayName("Mantiene modo OneDrive ENTREGABLES al crear")
        void crearActividad_modoOneDriveEntregables() {
            crearActividadDTO.setSubirAOneDrive(true);
            crearActividadDTO.setModoOneDrive(ModoOneDrive.ENTREGABLES);
            crearActividadDTO.setCarpetaOneDrive("/NoDebePersistirEnActividad");

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            actividadService.crearActividad(crearActividadDTO, 1L);

            verify(actividadRepository).save(argThat(a ->
                    a.getModoOneDrive() == ModoOneDrive.ENTREGABLES
                            && a.getCarpetaOneDrive() == null));
        }
    }

    @Nested
    @DisplayName("listarActividadesCurso")
    class ListarActividadesCurso {

        @Test
        @DisplayName("Lista actividades del curso")
        void listar_ok() {
            when(cursoRepository.existsById(1L)).thenReturn(true);
            when(actividadRepository.findByCursoId(1L)).thenReturn(List.of(actividad));
            when(mapper.toDTO(actividad)).thenReturn(actividadDTO);

            List<ActividadDTO> result = actividadService.listarActividadesCurso(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si curso no existe")
        void listar_cursoNoExiste() {
            when(cursoRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> actividadService.listarActividadesCurso(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Devuelve lista vacía si no hay actividades")
        void listar_sinActividades() {
            when(cursoRepository.existsById(1L)).thenReturn(true);
            when(actividadRepository.findByCursoId(1L)).thenReturn(List.of());

            List<ActividadDTO> result = actividadService.listarActividadesCurso(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("listarActividadesVisiblesGrupo")
    class ListarActividadesVisiblesGrupo {

        @Test
        @DisplayName("Lista actividades visibles de un grupo")
        void listar_ok() {
            when(grupoRepository.findById(1L)).thenReturn(Optional.of(grupo));
            when(actividadRepository.findByGrupoIdAndVisibilidad(1L, Visibilidad.VISIBLE))
                    .thenReturn(List.of(actividad));
            when(mapper.toDTO(actividad)).thenReturn(actividadDTO);

            List<ActividadDTO> result = actividadService.listarActividadesVisiblesGrupo(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si grupo no existe")
        void listar_grupoNoExiste() {
            when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> actividadService.listarActividadesVisiblesGrupo(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("obtenerActividadConEntregables")
    class ObtenerConEntregables {

        @Test
        @DisplayName("Obtiene actividad con entregables")
        void obtener_ok() {
            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(mapper.toDTOWithEntregables(actividad)).thenReturn(actividadDTO);

            ActividadDTO result = actividadService.obtenerActividadConEntregables(1L);

            assertThat(result).isNotNull();
            verify(mapper).toDTOWithEntregables(actividad);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void obtener_noExiste() {
            when(actividadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> actividadService.obtenerActividadConEntregables(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cambiarVisibilidad")
    class CambiarVisibilidad {

        @Test
        @DisplayName("Cambia a VISIBLE")
        void cambiar_aVisible() {
            actividad.setVisibilidad(Visibilidad.OCULTO);
            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(actividadRepository.save(any(Actividad.class))).thenReturn(actividad);
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            actividadService.cambiarVisibilidad(1L, Visibilidad.VISIBLE);

            assertThat(actividad.getVisibilidad()).isEqualTo(Visibilidad.VISIBLE);
        }

        @Test
        @DisplayName("Cambia a OCULTO")
        void cambiar_aOculto() {
            actividad.setVisibilidad(Visibilidad.VISIBLE);
            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(actividadRepository.save(any(Actividad.class))).thenReturn(actividad);
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            actividadService.cambiarVisibilidad(1L, Visibilidad.OCULTO);

            assertThat(actividad.getVisibilidad()).isEqualTo(Visibilidad.OCULTO);
        }

        @Test
        @DisplayName("Lanza excepción si actividad no existe")
        void cambiar_noExiste() {
            when(actividadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> actividadService.cambiarVisibilidad(99L, Visibilidad.VISIBLE))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("obtenerActividadPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("Obtiene actividad existente")
        void obtener_ok() {
            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(mapper.toDTO(actividad)).thenReturn(actividadDTO);

            ActividadDTO result = actividadService.obtenerActividadPorId(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void obtener_noExiste() {
            when(actividadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> actividadService.obtenerActividadPorId(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("actualizarActividad")
    class ActualizarActividad {

        @Test
        @DisplayName("Actualiza actividad correctamente")
        void actualizar_ok() {
            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(actividadRepository.save(any(Actividad.class))).thenReturn(actividad);
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            CrearActividadDTO updateDTO = CrearActividadDTO.builder()
                    .titulo("Práctica 1 Actualizada")
                    .tipoActividad(TipoActividad.NO_EVALUABLE)
                    .visibilidad(Visibilidad.OCULTO)
                    .build();

            ActividadDTO result = actividadService.actualizarActividad(1L, updateDTO);

            assertThat(result).isNotNull();
            verify(actividadRepository).save(any(Actividad.class));
        }

        @Test
        @DisplayName("Actualiza actividad con grupos")
        void actualizar_conGrupos() {
            Grupo g1 = Grupo.builder().id(1L).titulo("G1").curso(curso).build();
            CrearActividadDTO updateDTO = CrearActividadDTO.builder()
                    .titulo("Actualizada")
                    .tipoActividad(TipoActividad.EVALUABLE)
                    .grupoIds(List.of(1L))
                    .build();

            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(grupoRepository.findAllById(List.of(1L))).thenReturn(List.of(g1));
            when(actividadRepository.save(any(Actividad.class))).thenReturn(actividad);
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            actividadService.actualizarActividad(1L, updateDTO);

            verify(grupoRepository).findAllById(List.of(1L));
        }

        @Test
        @DisplayName("Lanza excepción si actividad no existe")
        void actualizar_noExiste() {
            when(actividadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> actividadService.actualizarActividad(99L, crearActividadDTO))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Mantiene modo OneDrive ENTREGABLES al actualizar")
        void actualizar_modoOneDriveEntregables() {
            CrearActividadDTO updateDTO = CrearActividadDTO.builder()
                .titulo("Práctica 1")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .subirAOneDrive(true)
                .modoOneDrive(ModoOneDrive.ENTREGABLES)
                .carpetaOneDrive("/NoDebePersistirEnActividad")
                .build();

            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(any(Actividad.class))).thenReturn(actividadDTO);

            actividadService.actualizarActividad(1L, updateDTO);

            verify(actividadRepository).save(argThat(a ->
                a.getModoOneDrive() == ModoOneDrive.ENTREGABLES
                    && a.getCarpetaOneDrive() == null));
        }
    }

    @Nested
    @DisplayName("eliminarActividad")
    class EliminarActividad {

        @Test
        @DisplayName("Elimina actividad existente")
        void eliminar_ok() {
            when(actividadRepository.existsById(1L)).thenReturn(true);

            actividadService.eliminarActividad(1L);

            verify(actividadRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void eliminar_noExiste() {
            when(actividadRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> actividadService.eliminarActividad(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarActividadesEnPlazo")
    class ListarEnPlazo {

        @Test
        @DisplayName("Lista actividades en plazo")
        void listar_ok() {
            when(actividadRepository.findByCursoIdAndFechaLimiteAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(actividad));
            when(mapper.toDTO(actividad)).thenReturn(actividadDTO);

            List<ActividadDTO> result = actividadService.listarActividadesEnPlazo(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("listarActividadesProximasLimite")
    class ListarProximas {

        @Test
        @DisplayName("Lista actividades próximas a vencer")
        void listar_ok() {
            when(actividadRepository.findByCursoIdAndFechaLimiteBetween(eq(1L), any(), any()))
                    .thenReturn(List.of(actividad));
            when(mapper.toDTO(actividad)).thenReturn(actividadDTO);

            List<ActividadDTO> result = actividadService.listarActividadesProximasLimite(1L, 7);

            assertThat(result).hasSize(1);
        }
    }
}
