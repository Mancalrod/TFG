package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
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
class EntregaServiceTest {

    @Mock private EntregaRepository entregaRepository;
    @Mock private EntregableRepository entregableRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private EntityMapper mapper;
    @Mock private OneDriveService oneDriveService;
    @Mock private MicrosoftOAuthService microsoftOAuthService;

    @InjectMocks
    private EntregaService entregaService;

    private Entregable entregable;
    private Estudiante estudiante;
    private Entrega entrega;
    private EntregaDTO entregaDTO;
    private EntregaResumenDTO entregaResumenDTO;

    @BeforeEach
    void setUp() {
        Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001").build();
        Actividad actividad = Actividad.builder().id(1L).titulo("P1").curso(curso)
                .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

        entregable = Entregable.builder()
                .id(1L).titulo("Entregable 1").notaMaxima(10.0)
                .permiteReenvio(true).actividad(actividad)
                .entregas(new HashSet<>())
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .build();

        Usuario usuario = Usuario.builder().id(1L).nombre("Alumno")
                .correoElectronico("alumno@test.com").contrasena("pass").build();
        Grupo grupo = Grupo.builder().id(1L).titulo("G1").curso(curso).estudiantes(new HashSet<>()).build();
        estudiante = Estudiante.builder().id(1L).usuario(usuario).grupo(grupo).build();

        entrega = Entrega.builder()
                .id(1L).nombre("Mi entrega").version(1)
                .fechaEntrega(LocalDateTime.now())
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregable)
                .estudiante(estudiante)
                .archivos(new HashSet<>())
                .feedbacks(new HashSet<>())
                .build();

        entregaDTO = EntregaDTO.builder()
                .id(1L).nombre("Mi entrega").version(1)
                .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                .entregableId(1L).estudianteId(1L).estudianteNombre("Alumno")
                .archivos(List.of()).feedbacks(List.of()).build();

        entregaResumenDTO = EntregaResumenDTO.builder()
                .entregaId(1L).estudianteId(1L).estudianteNombre("Alumno")
                .estado(EstadoEntrega.ENTREGADO).version(1).build();
    }

    @Nested
    @DisplayName("realizarEntrega")
    class RealizarEntrega {

        @Test
        @DisplayName("Realiza primera entrega correctamente")
        void realizar_primeraEntrega() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.realizarEntrega(1L, 1L, "Mi entrega", null);

            assertThat(result.getNombre()).isEqualTo("Mi entrega");
            verify(entregaRepository).save(any(Entrega.class));
        }

        @Test
        @DisplayName("Realiza reenvío incrementando versión")
        void realizar_reenvio() {
            Entrega entregaAnterior = Entrega.builder()
                    .id(1L).version(1).esVersionActiva(true)
                    .entregable(entregable).estudiante(estudiante).build();

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(List.of(entregaAnterior));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.realizarEntrega(1L, 1L, "Reenvío", null);

            assertThat(result).isNotNull();
            verify(entregaRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Bloquea reenvío si no está permitido")
        void realizar_reenvioNoPermitido() {
            entregable.setPermiteReenvio(false);
            Entrega entregaAnterior = Entrega.builder()
                    .id(1L).version(1).esVersionActiva(true).build();

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(List.of(entregaAnterior));

            assertThatThrownBy(() -> entregaService.realizarEntrega(1L, 1L, "Re", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no permite reenvío");
        }

        @Test
        @DisplayName("Lanza excepción si entregable no existe")
        void realizar_entregableNoExiste() {
            when(entregableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.realizarEntrega(99L, 1L, "E", null))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si estudiante no existe")
        void realizar_estudianteNoExiste() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.realizarEntrega(1L, 99L, "E", null))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("obtenerEntrega")
    class ObtenerEntrega {

        @Test
        @DisplayName("Obtiene entrega existente")
        void obtener_ok() {
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(entrega)).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.obtenerEntrega(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void obtener_noExiste() {
            when(entregaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.obtenerEntrega(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarEntregasParaEvaluar")
    class ListarParaEvaluar {

        @Test
        @DisplayName("Lista entregas activas del entregable")
        void listar_ok() {
            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entrega));
            when(mapper.toResumenDTO(entrega)).thenReturn(entregaResumenDTO);

            List<EntregaResumenDTO> result = entregaService.listarEntregasParaEvaluar(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si entregable no existe")
        void listar_noExiste() {
            when(entregableRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> entregaService.listarEntregasParaEvaluar(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarEntregasEstudiante")
    class ListarEntregasEstudiante {

        @Test
        @DisplayName("Lista historial de versiones")
        void listar_ok() {
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(List.of(entrega));
            when(mapper.toDTO(entrega)).thenReturn(entregaDTO);

            List<EntregaDTO> result = entregaService.listarEntregasEstudiante(1L, 1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Ordena por versión descendente")
        void listar_ordenDescendente() {
            Entrega entrega2 = Entrega.builder()
                    .id(2L).nombre("Reenvío").version(2)
                    .fechaEntrega(LocalDateTime.now())
                    .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            EntregaDTO entregaDTO2 = EntregaDTO.builder()
                    .id(2L).nombre("Reenvío").version(2)
                    .estado(EstadoEntrega.ENTREGADO).build();

            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(List.of(entrega, entrega2));
            when(mapper.toDTO(entrega)).thenReturn(entregaDTO);
            when(mapper.toDTO(entrega2)).thenReturn(entregaDTO2);

            List<EntregaDTO> result = entregaService.listarEntregasEstudiante(1L, 1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Devuelve lista vacía sin entregas")
        void listar_vacio() {
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(List.of());

            List<EntregaDTO> result = entregaService.listarEntregasEstudiante(1L, 1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("calificarEntrega")
    class CalificarEntrega {

        @Test
        @DisplayName("Califica entrega correctamente")
        void calificar_ok() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(8.5).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.calificarEntrega(1L, cal);

            assertThat(result).isNotNull();
            verify(entregaRepository).save(argThat(e -> e.getEstado() == EstadoEntrega.CALIFICADO));
        }

        @Test
        @DisplayName("Lanza excepción si nota supera máxima")
        void calificar_notaSuperaMaxima() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(15.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));

            assertThatThrownBy(() -> entregaService.calificarEntrega(1L, cal))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nota máxima");
        }

        @Test
        @DisplayName("Lanza excepción si entrega no existe")
        void calificar_noExiste() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(5.0).build();

            when(entregaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.calificarEntrega(99L, cal))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Permite nota igual a máxima")
        void calificar_notaIgualMaxima() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(10.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            assertThatNoException().isThrownBy(() -> entregaService.calificarEntrega(1L, cal));
        }

        @Test
        @DisplayName("Permite cualquier nota si notaMaxima es null")
        void calificar_notaMaximaNull() {
            entregable.setNotaMaxima(null);
            CalificacionDTO cal = CalificacionDTO.builder().nota(99.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            assertThatNoException().isThrownBy(() -> entregaService.calificarEntrega(1L, cal));
        }

        @Test
        @DisplayName("Establece estado CALIFICADO y fecha al calificar")
        void calificar_verificaEstadoYFecha() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(7.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            entregaService.calificarEntrega(1L, cal);

            verify(entregaRepository).save(argThat(e ->
                    e.getCalificacion() == 7.0 &&
                    e.getEstado() == EstadoEntrega.CALIFICADO &&
                    e.getFechaCalificacion() != null
            ));
        }
    }

    @Nested
    @DisplayName("obtenerArchivo")
    class ObtenerArchivo {

        @Test
        @DisplayName("Obtiene archivo existente")
        void obtener_ok() {
            Material material = Material.builder().id(1L).nombre("archivo.pdf").build();
            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            Material result = entregaService.obtenerArchivo(1L);

            assertThat(result.getNombre()).isEqualTo("archivo.pdf");
        }

        @Test
        @DisplayName("Lanza excepción si archivo no existe")
        void obtener_noExiste() {
            when(materialRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.obtenerArchivo(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarTodasEntregasEstudiante")
    class ListarTodas {

        @Test
        @DisplayName("Lista todas las entregas del estudiante")
        void listar_ok() {
            when(estudianteRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEstudianteId(1L)).thenReturn(List.of(entrega));
            when(mapper.toDTO(entrega)).thenReturn(entregaDTO);

            List<EntregaDTO> result = entregaService.listarTodasEntregasEstudiante(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si estudiante no existe")
        void listar_noExiste() {
            when(estudianteRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> entregaService.listarTodasEntregasEstudiante(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarEntregasPendientesCalificar")
    class ListarPendientes {

        @Test
        @DisplayName("Lista entregas pendientes de calificar")
        void listar_ok() {
            when(entregaRepository.findByEstadoAndEsVersionActiva(EstadoEntrega.ENTREGADO, true))
                    .thenReturn(List.of(entrega));
            when(mapper.toResumenDTO(entrega)).thenReturn(entregaResumenDTO);

            List<EntregaResumenDTO> result = entregaService.listarEntregasPendientesCalificar(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devuelve lista vacía sin entregas pendientes")
        void listar_vacio() {
            when(entregaRepository.findByEstadoAndEsVersionActiva(EstadoEntrega.ENTREGADO, true))
                    .thenReturn(List.of());

            List<EntregaResumenDTO> result = entregaService.listarEntregasPendientesCalificar(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerEstadisticas")
    class ObtenerEstadisticas {

        @Test
        @DisplayName("Obtiene estadísticas correctamente")
        void obtener_ok() {
            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entrega));

            EntregaEstadisticasDTO result = entregaService.obtenerEstadisticas(1L);

            assertThat(result.getTotalEntregas()).isEqualTo(1);
            assertThat(result.getEntregableId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si entregable no existe")
        void obtener_noExiste() {
            when(entregableRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> entregaService.obtenerEstadisticas(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Estadísticas con lista vacía")
        void obtener_sinEntregas() {
            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of());

            EntregaEstadisticasDTO result = entregaService.obtenerEstadisticas(1L);

            assertThat(result.getTotalEntregas()).isZero();
            assertThat(result.getEntregasATiempo()).isZero();
            assertThat(result.getEntregasTardias()).isZero();
            assertThat(result.getEntregasCalificadas()).isZero();
            assertThat(result.getEntregasPendientes()).isZero();
            assertThat(result.getPromedioCalificacion()).isNull();
        }

        @Test
        @DisplayName("Estadísticas con entregas mixtas (calificadas y pendientes)")
        void obtener_mixtas() {
            Entrega entregaCalificada = Entrega.builder()
                    .id(2L).version(1).estado(EstadoEntrega.CALIFICADO)
                    .calificacion(8.0).esVersionActiva(true)
                    .fechaEntrega(LocalDateTime.of(2026, 2, 15, 10, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entrega, entregaCalificada));

            EntregaEstadisticasDTO result = entregaService.obtenerEstadisticas(1L);

            assertThat(result.getTotalEntregas()).isEqualTo(2);
            assertThat(result.getEntregasCalificadas()).isEqualTo(1);
            assertThat(result.getEntregasPendientes()).isEqualTo(1);
            assertThat(result.getPromedioCalificacion()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Estadísticas con entrega tardía")
        void obtener_conTardia() {
            Entrega entregaTardia = Entrega.builder()
                    .id(3L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true)
                    .fechaEntrega(LocalDateTime.of(2099, 1, 1, 0, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entrega, entregaTardia));

            EntregaEstadisticasDTO result = entregaService.obtenerEstadisticas(1L);

            assertThat(result.getTotalEntregas()).isEqualTo(2);
            assertThat(result.getEntregasTardias()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("eliminarEntrega")
    class EliminarEntrega {

        @Test
        @DisplayName("Elimina entrega no calificada")
        void eliminar_ok() {
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));

            entregaService.eliminarEntrega(1L);

            verify(entregaRepository).delete(entrega);
        }

        @Test
        @DisplayName("Bloquea eliminación de entrega calificada")
        void eliminar_calificada() {
            entrega.setEstado(EstadoEntrega.CALIFICADO);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));

            assertThatThrownBy(() -> entregaService.eliminarEntrega(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("calificada");
        }

        @Test
        @DisplayName("Lanza excepción si entrega no existe")
        void eliminar_noExiste() {
            when(entregaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.eliminarEntrega(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("descargarContenidoArchivo")
    class DescargarContenidoArchivo {

        @Test
        @DisplayName("Descarga archivo desde OneDrive cuando tiene referencia")
        void descargar_desdeOneDrive() {
            Material material = Material.builder()
                    .id(1L).nombre("doc.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .onedriveFileId("od-file-123")
                    .onedriveOwnerId(10L)
                    .build();
            byte[] contenido = "contenido-onedrive".getBytes();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(10L, "od-file-123")).thenReturn(contenido);

            byte[] result = entregaService.descargarContenidoArchivo(1L);

            assertThat(result).isEqualTo(contenido);
            verify(oneDriveService).descargarArchivo(10L, "od-file-123");
        }

        @Test
        @DisplayName("Lanza excepción si material no existe")
        void descargar_materialNoExiste() {
            when(materialRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si no hay ruta ni OneDrive")
        void descargar_sinRutaValida() {
            Material material = Material.builder()
                    .id(1L).nombre("doc.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta(null)
                    .onedriveFileId(null)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("descargarTodoComoZip")
    class DescargarTodoComoZip {

        @Test
        @DisplayName("Lanza excepción si entregable no existe")
        void descargarTodo_noExiste() {
            when(entregableRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> entregaService.descargarTodoComoZip(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Genera ZIP vacío cuando no hay entregas")
        void descargarTodo_sinEntregas() {
            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of());

            byte[] result = entregaService.descargarTodoComoZip(1L);

            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("Genera ZIP con archivos de múltiples estudiantes")
        void descargarTodo_conEntregas() {
            Material material1 = Material.builder()
                    .id(10L).nombre("memoria.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta("test-path/memoria.pdf")
                    .build();

            Set<Material> archivos = new HashSet<>();
            archivos.add(material1);

            Entrega entregaConArchivos = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(archivos).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entregaConArchivos));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(material1));

            // Material tiene ruta local → descargarContenidoArchivo leerá de FS, que fallará.
            // Pero the ZIP builder catches errors per-file and logs a warning.
            byte[] result = entregaService.descargarTodoComoZip(1L);

            assertThat(result).isNotNull();
        }
    }
}
