package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
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
class EntregableServiceTest {

    @Mock private EntregableRepository entregableRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private EntregaRepository entregaRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private EntityMapper mapper;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private EntregableService entregableService;

    private Actividad actividad;
    private Entregable entregable;
    private EntregableDTO entregableDTO;
    private CrearEntregableDTO crearEntregableDTO;

    @BeforeEach
    void setUp() {
        Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001").build();

        actividad = Actividad.builder().id(1L).titulo("Práctica 1").curso(curso)
                .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

        entregable = Entregable.builder()
                .id(1L).titulo("Entregable 1").descripcion("Desc")
                .fechaInicio(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .notaMaxima(10.0)
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(5000000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividad)
                .entregas(new HashSet<>())
                .build();

        entregableDTO = EntregableDTO.builder()
                .id(1L).titulo("Entregable 1").actividadId(1L)
                .visibilidad(Visibilidad.VISIBLE).permiteReenvio(true)
                .numeroEntregas(0L).enPlazo(true).build();

        crearEntregableDTO = CrearEntregableDTO.builder()
                .titulo("Entregable 1").descripcion("Desc")
                .fechaInicio(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .notaMaxima(10.0).tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(5000000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true).build();
    }

    @Nested
    @DisplayName("crearEntregable")
    class CrearEntregable {

        @Test
        @DisplayName("Crea entregable correctamente")
        void crear_ok() {
            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(entregableRepository.save(any(Entregable.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(any(Entregable.class))).thenReturn(entregableDTO);

            EntregableDTO result = entregableService.crearEntregable(crearEntregableDTO, 1L);

            assertThat(result.getTitulo()).isEqualTo("Entregable 1");
            verify(entregableRepository).save(any(Entregable.class));
            verify(notificacionService).notificarNuevoEntregable(any(Entregable.class));
        }

        @Test
        @DisplayName("Lanza excepción si actividad no existe")
        void crear_actividadNoExiste() {
            when(actividadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregableService.crearEntregable(crearEntregableDTO, 99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Asigna visibilidad OCULTO por defecto si null")
        void crear_visibilidadDefault() {
            CrearEntregableDTO dtoSinVis = CrearEntregableDTO.builder()
                    .titulo("E").fechaLimite(LocalDateTime.now().plusDays(1))
                    .visibilidad(null).build();

            when(actividadRepository.findById(1L)).thenReturn(Optional.of(actividad));
            when(entregableRepository.save(any(Entregable.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(any(Entregable.class))).thenReturn(entregableDTO);

            entregableService.crearEntregable(dtoSinVis, 1L);

            verify(entregableRepository).save(argThat(e -> e.getVisibilidad() == Visibilidad.OCULTO));
            verify(notificacionService, never()).notificarNuevoEntregable(any(Entregable.class));
        }
    }

    @Nested
    @DisplayName("listarEntregablesActividad")
    class ListarEntregables {

        @Test
        @DisplayName("Lista entregables de actividad existente")
        void listar_ok() {
            when(actividadRepository.existsById(1L)).thenReturn(true);
            when(entregableRepository.findByActividadId(1L)).thenReturn(List.of(entregable));
            when(mapper.toDTO(entregable)).thenReturn(entregableDTO);

            List<EntregableDTO> result = entregableService.listarEntregablesActividad(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si actividad no existe")
        void listar_actividadNoExiste() {
            when(actividadRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> entregableService.listarEntregablesActividad(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("obtenerEntregable")
    class ObtenerEntregable {

        @Test
        @DisplayName("Obtiene entregable existente")
        void obtener_ok() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(mapper.toDTO(entregable)).thenReturn(entregableDTO);

            EntregableDTO result = entregableService.obtenerEntregable(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void obtener_noExiste() {
            when(entregableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregableService.obtenerEntregable(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarEntregablesVisibles")
    class ListarVisibles {

        @Test
        @DisplayName("Lista entregables visibles")
        void listar_ok() {
            when(actividadRepository.existsById(1L)).thenReturn(true);
            when(entregableRepository.findByActividadIdAndVisibilidad(1L, Visibilidad.VISIBLE))
                    .thenReturn(List.of(entregable));
            when(mapper.toDTO(entregable)).thenReturn(entregableDTO);

            List<EntregableDTO> result = entregableService.listarEntregablesVisibles(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si actividad no existe")
        void listar_actividadNoExiste() {
            when(actividadRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> entregableService.listarEntregablesVisibles(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("actualizarEntregable")
    class ActualizarEntregable {

        @Test
        @DisplayName("Actualiza entregable correctamente")
        void actualizar_ok() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(entregableRepository.save(any(Entregable.class))).thenReturn(entregable);
            when(mapper.toDTO(any(Entregable.class))).thenReturn(entregableDTO);

            EntregableDTO result = entregableService.actualizarEntregable(1L, crearEntregableDTO);

            assertThat(result).isNotNull();
            verify(entregableRepository).save(any(Entregable.class));
        }

        @Test
        @DisplayName("Lanza excepción si entregable no existe")
        void actualizar_noExiste() {
            when(entregableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregableService.actualizarEntregable(99L, crearEntregableDTO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cambiarVisibilidad")
    class CambiarVisibilidad {

        @Test
        @DisplayName("Cambia visibilidad a VISIBLE")
        void cambiar_aVisible() {
            entregable.setVisibilidad(Visibilidad.OCULTO);
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(entregableRepository.save(any(Entregable.class))).thenReturn(entregable);
            when(mapper.toDTO(any(Entregable.class))).thenReturn(entregableDTO);

            EntregableDTO result = entregableService.cambiarVisibilidad(1L, Visibilidad.VISIBLE);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Lanza excepción si entregable no existe")
        void cambiar_noExiste() {
            when(entregableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregableService.cambiarVisibilidad(99L, Visibilidad.VISIBLE))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("eliminarEntregable")
    class EliminarEntregable {

        @Test
        @DisplayName("Elimina entregable existente")
        void eliminar_ok() {
            when(entregableRepository.existsById(1L)).thenReturn(true);

            entregableService.eliminarEntregable(1L);

            verify(entregableRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void eliminar_noExiste() {
            when(entregableRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> entregableService.eliminarEntregable(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarEntregablesEnPlazo")
    class ListarEnPlazo {

        @Test
        @DisplayName("Lista entregables en plazo")
        void listar_ok() {
            when(entregableRepository.findByActividadIdAndFechaLimiteAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(entregable));
            when(mapper.toDTO(entregable)).thenReturn(entregableDTO);

            List<EntregableDTO> result = entregableService.listarEntregablesEnPlazo(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("listarEntregablesProximosVencer")
    class ListarProximos {

        @Test
        @DisplayName("Lista entregables próximos a vencer")
        void listar_ok() {
            when(entregableRepository.findByActividadIdAndFechaLimiteBetween(
                    eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(entregable));
            when(mapper.toDTO(entregable)).thenReturn(entregableDTO);

            List<EntregableDTO> result = entregableService.listarEntregablesProximosVencer(1L, 7);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("listarEntregablesPendientesEstudiante")
    class ListarPendientes {

        @Test
        @DisplayName("Lista entregables pendientes del estudiante")
        void listar_ok() {
            when(entregableRepository.findByActividadId(1L)).thenReturn(List.of(entregable));
            when(mapper.toDTO(entregable)).thenReturn(entregableDTO);

            List<EntregableDTO> result = entregableService.listarEntregablesPendientesEstudiante(1L, 1L);

            assertThat(result).hasSize(1);
        }
    }
}
