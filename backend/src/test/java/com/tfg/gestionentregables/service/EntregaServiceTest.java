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
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
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
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
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
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
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
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(99L, 1L)).thenReturn(Optional.empty());

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
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
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

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
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
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
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
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(List.of(estudiante));
            when(entregaRepository.findByEstudianteId(1L)).thenReturn(List.of(entrega));
            when(mapper.toDTO(entrega)).thenReturn(entregaDTO);

            List<EntregaDTO> result = entregaService.listarTodasEntregasEstudiante(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si estudiante no existe")
        void listar_noExiste() {
            when(estudianteRepository.findByUsuarioId(99L)).thenReturn(List.of());

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

    // =============================================
    // determinarTipoMaterial (método privado, via reflexión)
    // =============================================

    @Nested
    @DisplayName("determinarTipoMaterial")
    class DeterminarTipoMaterial {

        private TipoMaterial invocarDeterminar(String contentType) {
            try {
                Method method = EntregaService.class.getDeclaredMethod("determinarTipoMaterial", String.class);
                method.setAccessible(true);
                return (TipoMaterial) method.invoke(entregaService, contentType);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("PDF")
        void tipo_pdf() {
            assertThat(invocarDeterminar("application/pdf")).isEqualTo(TipoMaterial.PDF);
        }

        @Test
        @DisplayName("Imagen PNG")
        void tipo_imagen_png() {
            assertThat(invocarDeterminar("image/png")).isEqualTo(TipoMaterial.IMAGEN);
        }

        @Test
        @DisplayName("Imagen JPEG")
        void tipo_imagen_jpeg() {
            assertThat(invocarDeterminar("image/jpeg")).isEqualTo(TipoMaterial.IMAGEN);
        }

        @Test
        @DisplayName("ZIP")
        void tipo_zip() {
            assertThat(invocarDeterminar("application/zip")).isEqualTo(TipoMaterial.ZIP);
        }

        @Test
        @DisplayName("RAR")
        void tipo_rar() {
            assertThat(invocarDeterminar("application/x-rar-compressed")).isEqualTo(TipoMaterial.ZIP);
        }

        @Test
        @DisplayName("7z")
        void tipo_7z() {
            assertThat(invocarDeterminar("application/x-7z-compressed")).isEqualTo(TipoMaterial.ZIP);
        }

        @Test
        @DisplayName("Word .docx")
        void tipo_word() {
            assertThat(invocarDeterminar("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .isEqualTo(TipoMaterial.DOCX);
        }

        @Test
        @DisplayName("Word legacy .doc")
        void tipo_word_legacy() {
            assertThat(invocarDeterminar("application/msword")).isEqualTo(TipoMaterial.DOCX);
        }

        @Test
        @DisplayName("Null devuelve OTRO")
        void tipo_null() {
            assertThat(invocarDeterminar(null)).isEqualTo(TipoMaterial.OTRO);
        }

        @Test
        @DisplayName("Tipo desconocido devuelve OTRO")
        void tipo_desconocido() {
            assertThat(invocarDeterminar("application/octet-stream")).isEqualTo(TipoMaterial.OTRO);
        }

        @Test
        @DisplayName("Texto plano devuelve OTRO")
        void tipo_texto() {
            assertThat(invocarDeterminar("text/plain")).isEqualTo(TipoMaterial.OTRO);
        }
    }

    // =============================================
    // descargarContenidoArchivo
    // =============================================

    @Nested
    @DisplayName("descargarContenidoArchivo")
    class DescargarContenidoArchivo {

        @Test
        @DisplayName("Descarga desde OneDrive si tiene referencia")
        void descargar_onedrive() {
            Material material = Material.builder()
                    .id(1L).nombre("archivo.pdf")
                    .onedriveFileId("file-id-123")
                    .onedriveOwnerId(1L)
                    .ruta("onedrive://file-id-123")
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(1L, "file-id-123"))
                    .thenReturn("contenido pdf".getBytes());

            byte[] result = entregaService.descargarContenidoArchivo(1L);

            assertThat(result).isEqualTo("contenido pdf".getBytes());
            verify(oneDriveService).descargarArchivo(1L, "file-id-123");
        }

        @Test
        @DisplayName("Lanza excepción si material no existe")
        void descargar_materialNoExiste() {
            when(materialRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Archivo no encontrado");
        }

        @Test
        @DisplayName("Lanza excepción si OneDrive falla con ruta virtual")
        void descargar_onedriveFalla_rutaVirtual() {
            Material material = Material.builder()
                    .id(1L).nombre("archivo.pdf")
                    .onedriveFileId("file-id-123")
                    .onedriveOwnerId(1L)
                    .ruta("onedrive://file-id-123")
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(1L, "file-id-123"))
                    .thenThrow(new RuntimeException("Error de conexión"));

            assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No se pudo descargar");
        }

        @Test
        @DisplayName("Lanza excepción si no hay ruta válida")
        void descargar_sinRuta() {
            Material material = Material.builder()
                    .id(1L).nombre("archivo.pdf")
                    .ruta(null)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ruta de almacenamiento válida");
        }
    }

    // =============================================
    // realizarEntrega con archivos (OneDrive)
    // =============================================

    @Nested
    @DisplayName("realizarEntrega con archivos")
    class RealizarEntregaConArchivos {

        @Test
        @DisplayName("Sube archivos a OneDrive de profesor conectado")
        void realizar_conArchivos_oneDriveProfesor() {
            // Setup profesor con OneDrive conectado
            Usuario usuarioProf = Usuario.builder().id(10L).nombre("Prof. García")
                    .correoElectronico("prof@test.com").contrasena("pass").build();
            Profesor profesor = Profesor.builder().id(1L).usuario(usuarioProf).build();

            Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001")
                    .profesores(Set.of(profesor)).build();
            Actividad actividad = Actividad.builder().id(1L).titulo("P1").curso(curso)
                    .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

            Entregable entregableConCurso = Entregable.builder()
                    .id(1L).titulo("Entregable 1").notaMaxima(10.0)
                    .permiteReenvio(true).actividad(actividad)
                    .entregas(new HashSet<>())
                    .fechaLimite(LocalDateTime.now().plusDays(7))
                    .build();

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "trabajo.pdf", "application/pdf", "contenido del pdf".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregableConCurso));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            // OneDrive habilitado y profesor conectado
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(false); // alumno no conectado
            when(oneDriveService.subirArchivo(eq(10L), any(), eq("IS"), eq("P1"),
                    eq("Entregable 1"), eq("Alumno"), any()))
                    .thenReturn(Map.of("fileId", "od-file-123", "webUrl", "https://onedrive.live.com/file123"));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

            EntregaDTO result = entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(archivo));

            assertThat(result).isNotNull();
            verify(oneDriveService).subirArchivo(eq(10L), any(), eq("IS"), eq("P1"),
                    eq("Entregable 1"), eq("Alumno"), any());
            verify(materialRepository).save(any(Material.class));
        }

        @Test
        @DisplayName("Sube archivos a OneDrive del alumno si profesor no conectado")
        void realizar_conArchivos_oneDriveAlumno() {
            // Setup - ningún profesor con OneDrive
            Usuario usuarioProf = Usuario.builder().id(10L).nombre("Prof.")
                    .correoElectronico("prof@test.com").contrasena("pass").build();
            Profesor profesor = Profesor.builder().id(1L).usuario(usuarioProf).build();

            Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001")
                    .profesores(Set.of(profesor)).build();
            Actividad actividad = Actividad.builder().id(1L).titulo("P1").curso(curso)
                    .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

            Entregable entregableConCurso = Entregable.builder()
                    .id(1L).titulo("Entregable 1").notaMaxima(10.0)
                    .permiteReenvio(true).actividad(actividad)
                    .entregas(new HashSet<>())
                    .fechaLimite(LocalDateTime.now().plusDays(7))
                    .build();

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "trabajo.pdf", "application/pdf", "contenido".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregableConCurso));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            // OneDrive habilitado, profesor NO conectado, alumno SÍ conectado
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(false);
            when(oneDriveService.estaConectado(1L)).thenReturn(true); // alumno conectado
            when(oneDriveService.subirArchivo(eq(1L), any(), eq("IS"), eq("P1"),
                    eq("Entregable 1"), eq("Mis Entregas"), any()))
                    .thenReturn(Map.of("fileId", "od-alu-123", "webUrl", "https://onedrive.live.com/alu123"));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

            EntregaDTO result = entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(archivo));

            assertThat(result).isNotNull();
            verify(oneDriveService).subirArchivo(eq(1L), any(), eq("IS"), eq("P1"),
                    eq("Entregable 1"), eq("Mis Entregas"), any());
        }

        @Test
        @DisplayName("Sube a profesor y alumno cuando ambos conectados")
        void realizar_conArchivos_ambosConectados() {
            Usuario usuarioProf = Usuario.builder().id(10L).nombre("Prof.")
                    .correoElectronico("prof@test.com").contrasena("pass").build();
            Profesor profesor = Profesor.builder().id(1L).usuario(usuarioProf).build();

            Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001")
                    .profesores(Set.of(profesor)).build();
            Actividad actividad = Actividad.builder().id(1L).titulo("P1").curso(curso)
                    .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

            Entregable entregableConCurso = Entregable.builder()
                    .id(1L).titulo("Entregable 1").notaMaxima(10.0)
                    .permiteReenvio(true).actividad(actividad)
                    .entregas(new HashSet<>())
                    .fechaLimite(LocalDateTime.now().plusDays(7))
                    .build();

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "trabajo.pdf", "application/pdf", "contenido".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregableConCurso));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(true);
            when(oneDriveService.subirArchivo(eq(10L), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Map.of("fileId", "prof-file", "webUrl", "https://prof-url"));
            when(oneDriveService.subirArchivo(eq(1L), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Map.of("fileId", "alu-file", "webUrl", "https://alu-url"));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

            entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(archivo));

            // Verifica que se subió tanto al profesor como al alumno
            verify(oneDriveService).subirArchivo(eq(10L), any(), any(), any(), any(), any(), any());
            verify(oneDriveService).subirArchivo(eq(1L), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Continúa si subida a OneDrive de profesor falla")
        void realizar_conArchivos_errorSubidaProfesor() {
            Usuario usuarioProf = Usuario.builder().id(10L).nombre("Prof.")
                    .correoElectronico("prof@test.com").contrasena("pass").build();
            Profesor profesor = Profesor.builder().id(1L).usuario(usuarioProf).build();

            Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001")
                    .profesores(Set.of(profesor)).build();
            Actividad actividad = Actividad.builder().id(1L).titulo("P1").curso(curso)
                    .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

            Entregable entregableConCurso = Entregable.builder()
                    .id(1L).titulo("Entregable 1").notaMaxima(10.0)
                    .permiteReenvio(true).actividad(actividad)
                    .entregas(new HashSet<>())
                    .fechaLimite(LocalDateTime.now().plusDays(7))
                    .build();

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "trabajo.pdf", "application/pdf", "contenido".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregableConCurso));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(false);
            // subirArchivo del profesor lanza excepción
            when(oneDriveService.subirArchivo(eq(10L), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Error de red"));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

            // No lanza excepción, sino que hace fallback a local
            assertThatNoException().isThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(archivo)));
        }

        @Test
        @DisplayName("Usa almacenamiento local si OneDrive deshabilitado")
        void realizar_conArchivos_oneDriveDeshabilitado() {
            Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001")
                    .profesores(new HashSet<>()).build();
            Actividad actividad = Actividad.builder().id(1L).titulo("P1").curso(curso)
                    .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

            Entregable entregableLocal = Entregable.builder()
                    .id(1L).titulo("Entregable 1").notaMaxima(10.0)
                    .permiteReenvio(true).actividad(actividad)
                    .entregas(new HashSet<>())
                    .fechaLimite(LocalDateTime.now().plusDays(7))
                    .build();

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "trabajo.pdf", "application/pdf", "contenido".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregableLocal));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            // OneDrive deshabilitado - usará almacenamiento local
            when(oneDriveService.isEnabled()).thenReturn(false);
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

            EntregaDTO result = entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(archivo));

            assertThat(result).isNotNull();
            verify(oneDriveService, never()).subirArchivo(any(), any(), any(), any(), any(), any(), any());
            verify(materialRepository).save(any(Material.class));
        }
    }
}
