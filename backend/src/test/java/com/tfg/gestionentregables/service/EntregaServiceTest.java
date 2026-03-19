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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

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
    @Mock private ZipValidationService zipValidationService;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private UsuarioRepository usuarioRepository;

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

        @Test
        @DisplayName("Falla si archivo ZIP no cumple estructura esperada")
        void realizar_zipValidacionFalla() {
            entregable.setEstructuraZip("src/main.java");
            entregable.setNombreZipEsperado("entrega.zip");

            MockMultipartFile zipFile = new MockMultipartFile(
                    "file", "entrega.zip", "application/zip", new byte[]{1, 2, 3});

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(zipValidationService.validarZip(any(), eq("src/main.java"), eq(false), eq("entrega.zip")))
                    .thenReturn(new ZipValidationService.ResultadoValidacion(false, List.of("Falta src/main.java")));

            assertThatThrownBy(() -> entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(zipFile)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no cumple la estructura");
        }

            @Test
            @DisplayName("Falla si se adjuntan varios archivos cuando el entregable exige ZIP estructurado")
            void realizar_zipEstructurado_variosArchivosFalla() {
                entregable.setEstructuraZip("src/main.java");

                MockMultipartFile zipUno = new MockMultipartFile(
                    "file", "entrega1.zip", "application/zip", new byte[]{1, 2, 3});
                MockMultipartFile zipDos = new MockMultipartFile(
                    "file", "entrega2.zip", "application/zip", new byte[]{4, 5, 6});

                when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
                when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));

                assertThatThrownBy(() -> entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(zipUno, zipDos)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("único archivo ZIP");
            }

            @Test
            @DisplayName("Falla si el entregable exige ZIP estructurado y el archivo no es .zip")
            void realizar_zipEstructurado_archivoNoZipFalla() {
                entregable.setEstructuraZip("src/main.java");

                MockMultipartFile txt = new MockMultipartFile(
                    "file", "entrega.txt", "text/plain", "contenido".getBytes());

                when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
                when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));

                assertThatThrownBy(() -> entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(txt)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("extensión .zip");
            }

        @Test
        @DisplayName("Pasa validación ZIP y almacena localmente")
        void realizar_zipValidacionOkYAlmacenaLocal(@TempDir Path tempDir) {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            entregable.setEstructuraZip("src/main.java");
            entregable.setNombreZipEsperado("entrega.zip");

            MockMultipartFile zipFile = new MockMultipartFile(
                    "file", "entrega.zip", "application/zip", "contenido".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(zipValidationService.validarZip(any(), eq("src/main.java"), eq(false), eq("entrega.zip")))
                    .thenReturn(new ZipValidationService.ResultadoValidacion(true, List.of()));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(zipFile));

            assertThat(result).isNotNull();
            verify(materialRepository).save(any(Material.class));
        }

        @Test
        @DisplayName("No valida ZIP si nombre esperado es asterisco")
        void realizar_zipNombreAsterisco(@TempDir Path tempDir) {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            entregable.setNombreZipEsperado("*");
            entregable.setEstructuraZip(null);

            MockMultipartFile zipFile = new MockMultipartFile(
                    "file", "cualquier.zip", "application/zip", new byte[]{1, 2, 3});

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.realizarEntrega(1L, 1L, "Mi entrega", List.of(zipFile));

            assertThat(result).isNotNull();
            verifyNoInteractions(zipValidationService);
        }

        @Test
        @DisplayName("Elimina archivos OneDrive de versiones anteriores al reenviar")
        void realizar_reenvioConOneDriveEliminaAnterior() {
            Material matAnterior = Material.builder()
                    .id(5L).nombre("old.pdf")
                    .onedriveFileId("od-old-123")
                    .onedriveOwnerId(10L)
                    .build();
            Entrega entregaAnterior = Entrega.builder()
                    .id(2L).version(1).esVersionActiva(true)
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(matAnterior))
                    .build();
            entregable.getActividad().setSubirAOneDrive(true);

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(new ArrayList<>(List.of(entregaAnterior)));
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            entregaService.realizarEntrega(1L, 1L, "Reenvío", null);

            verify(oneDriveService).eliminarArchivo(10L, "od-old-123");
        }

        @Test
        @DisplayName("Sube a OneDrive manteniendo el nombre original del archivo")
        void realizar_onedriveMantieneNombreOriginal() {
            entregable.getActividad().setSubirAOneDrive(true);
            entregable.getActividad().setOneDriveUsuarioId(10L);
            entregable.getActividad().setCarpetaOneDrive("Entregas");

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "mi documento final.pdf", "application/pdf", "contenido".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L)).thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(false);
            when(oneDriveService.subirArchivo(
                    eq(10L),
                    any(),
                    anyString(),
                    anyString(),
                    anyString(),
                    anyString(),
                    eq("mi documento final.pdf"),
                    eq("Entregas")))
                    .thenReturn(Map.of("fileId", "od-file", "webUrl", "https://onedrive.example.com/file"));

            entregaService.realizarEntrega(1L, 1L, "Entrega", List.of(archivo));

            verify(oneDriveService).subirArchivo(
                    eq(10L),
                    any(),
                    anyString(),
                    anyString(),
                    anyString(),
                    anyString(),
                    eq("mi documento final.pdf"),
                    eq("Entregas"));
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

        @Test
        @DisplayName("Lanza excepción si entregable no existe")
        void listar_entregableNoExiste() {
            when(entregableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.listarEntregasEstudiante(99L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si estudiante no existe en el curso")
        void listar_estudianteNoExiste() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.listarEntregasEstudiante(1L, 99L))
                    .isInstanceOf(EntityNotFoundException.class);
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

            EntregaDTO result = entregaService.calificarEntrega(1L, 1L, cal);

            assertThat(result).isNotNull();
            verify(entregaRepository).save(argThat(e -> e.getEstado() == EstadoEntrega.CALIFICADO));
        }

        @Test
        @DisplayName("Lanza excepción si nota supera máxima")
        void calificar_notaSuperaMaxima() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(15.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));

            assertThatThrownBy(() -> entregaService.calificarEntrega(1L, 1L, cal))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nota máxima");
        }

        @Test
        @DisplayName("Lanza excepción si entrega no existe")
        void calificar_noExiste() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(5.0).build();

            when(entregaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.calificarEntrega(99L, 1L, cal))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Permite nota igual a máxima")
        void calificar_notaIgualMaxima() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(10.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            assertThatNoException().isThrownBy(() -> entregaService.calificarEntrega(1L, 1L, cal));
        }

        @Test
        @DisplayName("Permite cualquier nota si notaMaxima es null")
        void calificar_notaMaximaNull() {
            entregable.setNotaMaxima(null);
            CalificacionDTO cal = CalificacionDTO.builder().nota(99.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            assertThatNoException().isThrownBy(() -> entregaService.calificarEntrega(1L, 1L, cal));
        }

        @Test
        @DisplayName("Establece estado CALIFICADO y fecha al calificar")
        void calificar_verificaEstadoYFecha() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(7.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            entregaService.calificarEntrega(1L, 1L, cal);

            verify(entregaRepository).save(argThat(e ->
                    e.getCalificacion() == 7.0 &&
                    e.getEstado() == EstadoEntrega.CALIFICADO &&
                    e.getFechaCalificacion() != null
            ));
        }

        @Test
        @DisplayName("Crea feedback automático si se incluye comentario")
        void calificar_conComentario_creaFeedback() {
            CalificacionDTO cal = CalificacionDTO.builder()
                    .nota(8.0)
                    .comentario("Buen trabajo, pero faltan detalles")
                    .build();
            Usuario profesor = Usuario.builder().id(10L).nombre("Profesor").build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(usuarioRepository.findById(10L)).thenReturn(Optional.of(profesor));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            entregaService.calificarEntrega(1L, 10L, cal);

            verify(feedbackRepository).save(argThat(fb ->
                    fb.getComentario().equals("Buen trabajo, pero faltan detalles") &&
                    fb.getProfesor().equals(profesor)
            ));
        }

        @Test
        @DisplayName("No crea feedback si comentario está vacío")
        void calificar_sinComentario_noFeedback() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(8.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            entregaService.calificarEntrega(1L, 1L, cal);

            verify(feedbackRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza excepción si la nota es null")
        void calificar_notaNull() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(null).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));

            assertThatThrownBy(() -> entregaService.calificarEntrega(1L, 1L, cal))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nota es obligatoria");
        }

        @Test
        @DisplayName("Lanza excepción si la nota es negativa")
        void calificar_notaNegativa() {
            CalificacionDTO cal = CalificacionDTO.builder().nota(-1.0).build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));

            assertThatThrownBy(() -> entregaService.calificarEntrega(1L, 1L, cal))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede ser negativa");
        }
    }

    @Nested
    @DisplayName("realizarEntrega con comentario")
    class RealizarEntregaConComentario {

        @Test
        @DisplayName("Permite entrega solo con comentario (sin archivos)")
        void entrega_soloComentario() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());
            when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> {
                Entrega e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.realizarEntrega(
                    1L, 1L, null, "Este es mi comentario de entrega", null);

            assertThat(result).isNotNull();
            verify(entregaRepository).save(argThat(e ->
                    e.getComentarioAlumno().equals("Este es mi comentario de entrega")
            ));
        }

        @Test
        @DisplayName("Permite entrega solo con comentario cuando tipo esperado es OTRO")
        void entrega_soloComentario_tipoOtro() {
            entregable.setTipoArchivoEsperado(TipoMaterial.OTRO);

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());
            when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> {
                Entrega e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.realizarEntrega(
                    1L, 1L, null, "Comentario sin archivo", null);

            assertThat(result).isNotNull();
            verify(entregaRepository).save(argThat(e ->
                    "Comentario sin archivo".equals(e.getComentarioAlumno())
            ));
        }

        @Test
        @DisplayName("Permite entrega solo con comentario cuando tipo esperado es ENLACE")
        void entrega_soloComentario_tipoEnlace() {
            entregable.setTipoArchivoEsperado(TipoMaterial.ENLACE);

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());
            when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> {
                Entrega e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.realizarEntrega(
                    1L, 1L, null, "https://example.com/mi-entrega", null);

            assertThat(result).isNotNull();
            verify(entregaRepository).save(argThat(e ->
                    "https://example.com/mi-entrega".equals(e.getComentarioAlumno())
            ));
        }

        @Test
        @DisplayName("Falla entrega solo con comentario cuando tipo esperado requiere archivo")
        void entrega_soloComentario_tipoPdf_falla() {
            entregable.setTipoArchivoEsperado(TipoMaterial.PDF);

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));

            assertThatThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, null, "Solo comentario", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debes adjuntar al menos un archivo");
        }

        @Test
        @DisplayName("Falla si tipo esperado es SOLO_TEXTO y se adjuntan archivos")
        void entrega_soloTexto_conArchivo_falla() {
            entregable.setTipoArchivoEsperado(TipoMaterial.SOLO_TEXTO);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "entrega.txt", "text/plain", "contenido".getBytes());

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));

            assertThatThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, null, "Texto", List.of(file)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo texto");
        }

        @Test
        @DisplayName("Falla si tipo esperado es SOLO_TEXTO y no hay comentario")
        void entrega_soloTexto_sinComentario_falla() {
            entregable.setTipoArchivoEsperado(TipoMaterial.SOLO_TEXTO);

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));

            assertThatThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, "Entrega", "   ", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debes escribir un comentario");
        }

        @Test
        @DisplayName("Falla si no hay ni archivos ni comentario")
        void entrega_vacia_falla() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));

            assertThatThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("al menos un archivo o escribir un comentario");
        }

        @Test
        @DisplayName("Falla si comentario está en blanco y no hay archivos")
        void entrega_comentarioBlanco_falla() {
            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(anyLong(), anyLong()))
                    .thenReturn(Optional.of(estudiante));

            assertThatThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, null, "   ", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("al menos un archivo o escribir un comentario");
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

        @Test
        @DisplayName("Descarga desde almacenamiento local cuando no tiene OneDrive")
        void descargar_desdeLocal(@TempDir Path tempDir) throws Exception {
            Path archivo = tempDir.resolve("test.pdf");
            Files.write(archivo, "contenido-local".getBytes());

            Material material = Material.builder()
                    .id(1L).nombre("test.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta(archivo.toString())
                    .onedriveFileId(null)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            byte[] result = entregaService.descargarContenidoArchivo(1L);

            assertThat(result).isEqualTo("contenido-local".getBytes());
        }

        @Test
        @DisplayName("OneDrive falla con fallback a ruta local")
        void descargar_oneDriveFallbackLocal(@TempDir Path tempDir) throws Exception {
            Path archivo = tempDir.resolve("fallback.pdf");
            Files.write(archivo, "contenido-fallback".getBytes());

            Material material = Material.builder()
                    .id(1L).nombre("fallback.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta(archivo.toString())
                    .onedriveFileId("od-123")
                    .onedriveOwnerId(10L)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(10L, "od-123"))
                    .thenThrow(new RuntimeException("OneDrive no disponible"));

            byte[] result = entregaService.descargarContenidoArchivo(1L);

            assertThat(result).isEqualTo("contenido-fallback".getBytes());
        }

        @Test
        @DisplayName("OneDrive falla y ruta virtual lanza excepción")
        void descargar_oneDriveFallaSinFallback() {
            Material material = Material.builder()
                    .id(1L).nombre("doc.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta("onedrive://od-123")
                    .onedriveFileId("od-123")
                    .onedriveOwnerId(10L)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(10L, "od-123"))
                    .thenThrow(new RuntimeException("OneDrive no disponible"));

            assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No se pudo descargar");
        }

            @Test
            @DisplayName("Descarga desde Cloudinary cuando tiene referencia")
            void descargar_desdeCloudinary() {
                Material material = Material.builder()
                    .id(1L).nombre("doc.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .cloudinaryPublicId("pub-1")
                    .cloudinaryUrl("https://cloudinary.example.com/doc.pdf")
                    .build();
                byte[] contenido = "contenido-cloudinary".getBytes();

                when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
                when(cloudinaryService.descargarArchivo("https://cloudinary.example.com/doc.pdf")).thenReturn(contenido);

                byte[] result = entregaService.descargarContenidoArchivo(1L);

                assertThat(result).isEqualTo(contenido);
            }

            @Test
            @DisplayName("Cloudinary falla y propaga excepción de descarga")
            void descargar_cloudinaryFalla() {
                Material material = Material.builder()
                    .id(1L).nombre("doc.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .cloudinaryPublicId("pub-1")
                    .cloudinaryUrl("https://cloudinary.example.com/doc.pdf")
                    .build();

                when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
                when(cloudinaryService.descargarArchivo("https://cloudinary.example.com/doc.pdf"))
                    .thenThrow(new RuntimeException("Cloudinary no disponible"));

                assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cloudinary");
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

    @Nested
    @DisplayName("descargarTodoActividadComoZip")
    class DescargarTodoActividadComoZip {

        @Test
        @DisplayName("Lanza excepción si no hay entregables para la actividad")
        void descargar_sinEntregables() {
            when(entregableRepository.findByActividadId(99L)).thenReturn(List.of());

            assertThatThrownBy(() -> entregaService.descargarTodoActividadComoZip(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No se encontraron entregables");
        }

        @Test
        @DisplayName("Genera ZIP vacío cuando no hay entregas activas")
        void descargar_sinEntregas() {
            when(entregableRepository.findByActividadId(1L)).thenReturn(List.of(entregable));
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of());

            byte[] result = entregaService.descargarTodoActividadComoZip(1L);

            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("Genera ZIP con estructura entregable/estudiante")
        void descargar_conEntregas(@TempDir Path tempDir) throws Exception {
            Path archivo = tempDir.resolve("memoria.pdf");
            Files.write(archivo, "contenido-pdf".getBytes());

            Material material = Material.builder()
                    .id(10L).nombre("memoria.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta(archivo.toString())
                    .build();

            Entrega entregaConArchivos = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(material)).feedbacks(new HashSet<>()).build();

            when(entregableRepository.findByActividadId(1L)).thenReturn(List.of(entregable));
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entregaConArchivos));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(material));

            byte[] result = entregaService.descargarTodoActividadComoZip(1L);

            assertThat(result).isNotNull();
            // Verificar que el ZIP contiene la entrada con carpeta del entregable
            List<String> entryNames = new ArrayList<>();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(result))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    entryNames.add(entry.getName());
                }
            }
            assertThat(entryNames).anyMatch(n -> n.contains("Entregable 1") && n.contains("Alumno"));
        }
    }

    @Nested
    @DisplayName("listarContenidoZip")
    class ListarContenidoZip {

        @Test
        @DisplayName("Lista entradas de un ZIP")
        void listar_ok() throws Exception {
            // Crear un ZIP real en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("src/"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("src/Main.java"));
                zos.write("public class Main {}".getBytes());
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("README.md"));
                zos.write("# Readme".getBytes());
                zos.closeEntry();
            }

            Material material = Material.builder()
                    .id(1L).nombre("entrega.zip")
                    .tipoMaterial(TipoMaterial.ZIP)
                    .ruta(null)
                    .onedriveFileId("od-zip-1")
                    .onedriveOwnerId(10L)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(10L, "od-zip-1")).thenReturn(baos.toByteArray());

            List<Map<String, Object>> result = entregaService.listarContenidoZip(1L);

            assertThat(result).hasSize(3);
            assertThat(result).anyMatch(m -> "src/".equals(m.get("nombre")) && Boolean.TRUE.equals(m.get("esCarpeta")));
            assertThat(result).anyMatch(m -> "src/Main.java".equals(m.get("nombre")) && Boolean.FALSE.equals(m.get("esCarpeta")));
            assertThat(result).anyMatch(m -> "README.md".equals(m.get("nombre")));
        }

        @Test
        @DisplayName("Lanza excepción si material no existe")
        void listar_materialNoExiste() {
            when(materialRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entregaService.listarContenidoZip(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Rechaza ZIP con path traversal")
        void listar_pathTraversal() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("../../../etc/passwd"));
                zos.write("malicious".getBytes());
                zos.closeEntry();
            }

            Material material = Material.builder()
                    .id(1L).nombre("evil.zip")
                    .tipoMaterial(TipoMaterial.ZIP)
                    .ruta(null)
                    .onedriveFileId("od-evil")
                    .onedriveOwnerId(10L)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(10L, "od-evil")).thenReturn(baos.toByteArray());

            assertThatThrownBy(() -> entregaService.listarContenidoZip(1L))
                    .isInstanceOf(UncheckedIOException.class);
        }
    }

    @Nested
    @DisplayName("helpers privados y compatibilidad")
    class HelpersPrivadosYCompatibilidad {

        @Test
        @DisplayName("calificarEntrega(entregaId, calificacion) usa compatibilidad sin feedback")
        void calificar_overloadSinProfesor_noCreaFeedback() {
            CalificacionDTO cal = CalificacionDTO.builder()
                    .nota(8.0)
                    .comentario("Comentario del profesor")
                    .build();

            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            EntregaDTO result = entregaService.calificarEntrega(1L, cal);

            assertThat(result).isNotNull();
            verify(feedbackRepository, never()).save(any());
        }

        @Test
        @DisplayName("guardarEnCloudinary construye material con metadatos correctos")
        @SuppressWarnings("unchecked")
        void guardarEnCloudinary_ok() throws Exception {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "informe.pdf", "application/pdf", "contenido".getBytes());

            Map<String, String> cloudResult = Map.of(
                    "publicId", "tfg-entregables/IS-001/P1/Entregable 1/informe",
                    "secureUrl", "https://res.cloudinary.com/demo/raw/upload/informe");
            when(cloudinaryService.subirArchivo(any(), anyString())).thenReturn(cloudResult);

            Method method = EntregaService.class.getDeclaredMethod(
                    "guardarEnCloudinary", org.springframework.web.multipart.MultipartFile.class,
                    Entrega.class, Entregable.class, String.class);
            method.setAccessible(true);

            Material material = (Material) method.invoke(entregaService, archivo, entrega, entregable, "informe.pdf");

            assertThat(material.getNombre()).isEqualTo("informe.pdf");
            assertThat(material.getRuta()).isEqualTo("cloudinary://tfg-entregables/IS-001/P1/Entregable 1/informe");
            assertThat(material.getCloudinaryPublicId()).isEqualTo("tfg-entregables/IS-001/P1/Entregable 1/informe");
            assertThat(material.getCloudinaryUrl()).isEqualTo("https://res.cloudinary.com/demo/raw/upload/informe");
            assertThat(material.getTipoMaterial()).isEqualTo(TipoMaterial.PDF);
            assertThat(material.getTamanoBytes()).isEqualTo(archivo.getSize());
        }

        @Test
        @DisplayName("leerArchivoLocal lee archivo cuando uploadBaseDir está vacío")
        void leerArchivoLocal_uploadBaseDirVacio(@TempDir Path tempDir) throws Exception {
            Path file = tempDir.resolve("local.txt");
            Files.write(file, "hola-local".getBytes());
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", "");

            Method method = EntregaService.class.getDeclaredMethod("leerArchivoLocal", String.class);
            method.setAccessible(true);
            byte[] contenido = (byte[]) method.invoke(entregaService, file.toString());

            assertThat(contenido).isEqualTo("hola-local".getBytes());
        }

        @Test
        @DisplayName("leerArchivoLocal rechaza ruta fuera del directorio si no existe")
        void leerArchivoLocal_rutaFueraNoExiste(@TempDir Path tempDir) throws Exception {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            String rutaInexistenteFuera = tempDir.getParent().resolve("fuera-no-existe.txt").toString();

            Method method = EntregaService.class.getDeclaredMethod("leerArchivoLocal", String.class);
            method.setAccessible(true);

                InvocationTargetException ex = (InvocationTargetException) catchThrowable(
                    () -> method.invoke(entregaService, rutaInexistenteFuera));

                assertThat(ex).isNotNull();
                assertThat(ex.getCause())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fuera del directorio");
        }

        @Test
        @DisplayName("evitarColisionZip agrega sufijos incrementales cuando hay duplicados")
        @SuppressWarnings("unchecked")
        void evitarColisionZip_incremental() throws Exception {
            Method method = EntregaService.class.getDeclaredMethod("evitarColisionZip", String.class, Set.class);
            method.setAccessible(true);

            Set<String> used = new HashSet<>();
            used.add("Alumno/archivo.txt");

            String segundo = (String) method.invoke(entregaService, "Alumno/archivo.txt", used);
            String tercero = (String) method.invoke(entregaService, "Alumno/archivo.txt", used);

            assertThat(segundo).isEqualTo("Alumno/archivo (2).txt");
            assertThat(tercero).isEqualTo("Alumno/archivo (3).txt");
        }

        @Test
        @DisplayName("leerArchivoLocal permite ruta fuera del baseDir si el archivo existe")
        void leerArchivoLocal_fueraBasePeroExiste(@TempDir Path tempDir) throws Exception {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());

            Path externo = Files.createTempFile("ext-file", ".txt");
            Files.write(externo, "externo-ok".getBytes());

            try {
                Method method = EntregaService.class.getDeclaredMethod("leerArchivoLocal", String.class);
                method.setAccessible(true);

                byte[] contenido = (byte[]) method.invoke(entregaService, externo.toString());
                assertThat(contenido).isEqualTo("externo-ok".getBytes());
            } finally {
                Files.deleteIfExists(externo);
            }
        }

        @Test
        @DisplayName("guardarArchivoLocal rechaza path traversal en el nombre")
        void guardarArchivoLocal_pathTraversal(@TempDir Path tempDir) throws Exception {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            entrega.setId(123L);

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "doc.txt", "text/plain", "contenido".getBytes());

            Method method = EntregaService.class.getDeclaredMethod(
                    "guardarArchivoLocal",
                    org.springframework.web.multipart.MultipartFile.class,
                    Entrega.class,
                    String.class,
                    String.class);
            method.setAccessible(true);

                InvocationTargetException ex = (InvocationTargetException) catchThrowable(
                    () -> method.invoke(entregaService, archivo, entrega, "doc.txt", "..\\..\\evil.txt"));

                assertThat(ex).isNotNull();
                assertThat(ex.getCause())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ruta de archivo no válida");
        }

        @Test
        @DisplayName("sanitizarSegmentoZip cubre nulos y caracteres peligrosos")
        void sanitizarSegmentoZip_ramas() throws Exception {
            Method method = EntregaService.class.getDeclaredMethod("sanitizarSegmentoZip", String.class);
            method.setAccessible(true);

            String nulo = (String) method.invoke(entregaService, new Object[]{null});
            String vacio = (String) method.invoke(entregaService, "   ");
            String sucio = (String) method.invoke(entregaService, "a/b\\c:*?<>|");

            assertThat(nulo).isEqualTo("sin_nombre");
            assertThat(vacio).isEqualTo("sin_nombre");
            assertThat(sucio).isNotBlank();
            assertThat(sucio).doesNotContain("/").doesNotContain("\\");
        }

        @Test
        @DisplayName("esEntradaZipPeligrosa detecta null, absoluto y traversal")
        void esEntradaZipPeligrosa_ramas() throws Exception {
            Method method = EntregaService.class.getDeclaredMethod("esEntradaZipPeligrosa", String.class);
            method.setAccessible(true);

            boolean nulo = (boolean) method.invoke(entregaService, new Object[]{null});
            boolean unixAbs = (boolean) method.invoke(entregaService, "/etc/passwd");
            boolean winAbs = (boolean) method.invoke(entregaService, "\\\\windows\\system32");
            boolean traversal = (boolean) method.invoke(entregaService, "a/../b");
            boolean seguro = (boolean) method.invoke(entregaService, "src/Main.java");

            assertThat(nulo).isTrue();
            assertThat(unixAbs).isTrue();
            assertThat(winAbs).isTrue();
            assertThat(traversal).isTrue();
            assertThat(seguro).isFalse();
        }
    }

    @Nested
    @DisplayName("determinarTipoMaterial (private)")
    class DeterminarTipoMaterialTest {

        private TipoMaterial invocarDeterminarTipo(String contentType) {
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
        @DisplayName("Detecta PDF")
        void tipo_pdf() {
            assertThat(invocarDeterminarTipo("application/pdf")).isEqualTo(TipoMaterial.PDF);
        }

        @Test
        @DisplayName("Detecta imagen")
        void tipo_imagen() {
            assertThat(invocarDeterminarTipo("image/png")).isEqualTo(TipoMaterial.IMAGEN);
            assertThat(invocarDeterminarTipo("image/jpeg")).isEqualTo(TipoMaterial.IMAGEN);
        }

        @Test
        @DisplayName("Detecta ZIP")
        void tipo_zip() {
            assertThat(invocarDeterminarTipo("application/zip")).isEqualTo(TipoMaterial.ZIP);
            assertThat(invocarDeterminarTipo("application/x-rar-compressed")).isEqualTo(TipoMaterial.ZIP);
            assertThat(invocarDeterminarTipo("application/x-7z-compressed")).isEqualTo(TipoMaterial.ZIP);
        }

        @Test
        @DisplayName("Detecta DOCX")
        void tipo_docx() {
            assertThat(invocarDeterminarTipo("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).isEqualTo(TipoMaterial.DOCX);
        }

        @Test
        @DisplayName("Devuelve OTRO para tipo desconocido")
        void tipo_otro() {
            assertThat(invocarDeterminarTipo("text/plain")).isEqualTo(TipoMaterial.OTRO);
        }

        @Test
        @DisplayName("Devuelve OTRO para null")
        void tipo_null() {
            assertThat(invocarDeterminarTipo(null)).isEqualTo(TipoMaterial.OTRO);
        }
    }
}
