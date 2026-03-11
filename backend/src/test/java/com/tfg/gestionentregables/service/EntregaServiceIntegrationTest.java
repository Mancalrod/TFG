package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.EntregaDTO;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests complementarios de EntregaService que cubren los flujos de
 * subida a OneDrive (guardarArchivoConOneDrive / guardarEnOneDrive),
 * almacenamiento local con archivos reales, y generación de ZIPs
 * con contenido descargable verificable.
 */
@ExtendWith(MockitoExtension.class)
class EntregaServiceIntegrationTest {

    @Mock private EntregaRepository entregaRepository;
    @Mock private EntregableRepository entregableRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private EntityMapper mapper;
    @Mock private OneDriveService oneDriveService;
    @Mock private ZipValidationService zipValidationService;

    @InjectMocks
    private EntregaService entregaService;

    private Curso curso;
    private Actividad actividad;
    private Entregable entregable;
    private Usuario usuario;
    private Grupo grupo;
    private Estudiante estudiante;
    private Entrega entrega;
    private EntregaDTO entregaDTO;

    @BeforeEach
    void setUp() {
        curso = Curso.builder().id(1L).titulo("Curso Test").codigo("CT-001").build();
        actividad = Actividad.builder().id(1L).titulo("Actividad 1").curso(curso)
                .grupos(new HashSet<>()).entregables(new HashSet<>()).build();

        entregable = Entregable.builder()
                .id(1L).titulo("Entregable 1").notaMaxima(10.0)
                .permiteReenvio(true).actividad(actividad)
                .entregas(new HashSet<>())
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .build();

        usuario = Usuario.builder().id(1L).nombre("Alumno Test")
                .correoElectronico("alumno@test.com").contrasena("pass").build();
        grupo = Grupo.builder().id(1L).titulo("G1").curso(curso)
                .estudiantes(new HashSet<>()).build();
        estudiante = Estudiante.builder().id(1L).usuario(usuario).grupo(grupo).build();

        entrega = Entrega.builder()
                .id(1L).nombre("Mi entrega").version(1)
                .fechaEntrega(LocalDateTime.now())
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregable).estudiante(estudiante)
                .archivos(new HashSet<>()).feedbacks(new HashSet<>())
                .build();

        entregaDTO = EntregaDTO.builder()
                .id(1L).nombre("Mi entrega").version(1)
                .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                .entregableId(1L).estudianteId(1L).estudianteNombre("Alumno Test")
                .archivos(List.of()).feedbacks(List.of()).build();
    }

    /** Configura mocks base para que realizarEntrega llegue al procesamiento de archivos. */
    private void prepararMocksBaseRealizarEntrega() {
        when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
        when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L))
                .thenReturn(Optional.of(estudiante));
        when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                .thenReturn(List.of());
        when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
        when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);
    }

    // =====================================================================
    // SUBIDA A ONEDRIVE (guardarArchivoConOneDrive / guardarEnOneDrive)
    // =====================================================================

    @Nested
    @DisplayName("OneDrive upload a través de realizarEntrega")
    class OneDriveUpload {

        private MockMultipartFile archivo;

        @BeforeEach
        void init(@TempDir Path tempDir) {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            archivo = new MockMultipartFile(
                    "file", "informe.pdf", "application/pdf", "contenido-pdf".getBytes());
        }

        @Test
        @DisplayName("Sube al OneDrive del profesor cuando está habilitado y conectado")
        void subeOneDriveProfesor() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(false);
            when(oneDriveService.subirArchivo(eq(10L), any(), eq("Curso Test"),
                    eq("Actividad 1"), eq("Entregable 1"), eq("Alumno Test"), anyString()))
                    .thenReturn(Map.of("fileId", "prof-file-1", "webUrl", "https://onedrive/prof-file-1"));

            entregaService.realizarEntrega(1L, 1L, "Entrega OD", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isEqualTo("prof-file-1");
            assertThat(mat.getOnedriveOwnerId()).isEqualTo(10L);
            assertThat(mat.getRuta()).startsWith("onedrive://");
        }

        @Test
        @DisplayName("Sube al OneDrive del alumno cuando profesor no está conectado")
        void subeOneDriveAlumno() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(false);
            when(oneDriveService.estaConectado(1L)).thenReturn(true);
            when(oneDriveService.subirArchivo(eq(1L), any(), eq("Curso Test"),
                    eq("Actividad 1"), eq("Entregable 1"), eq("Mis Entregas"), anyString()))
                    .thenReturn(Map.of("fileId", "alum-file-1", "webUrl", "https://onedrive/alum-file-1"));

            entregaService.realizarEntrega(1L, 1L, "Entrega OD alumno", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isEqualTo("alum-file-1");
            assertThat(mat.getOnedriveOwnerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Sube a ambos OneDrive; material referencia al del profesor")
        void subeOneDriveAmbos() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(true);
            when(oneDriveService.subirArchivo(eq(10L), any(), anyString(),
                    anyString(), anyString(), eq("Alumno Test"), anyString()))
                    .thenReturn(Map.of("fileId", "prof-f", "webUrl", "https://od/prof"));
            when(oneDriveService.subirArchivo(eq(1L), any(), anyString(),
                    anyString(), anyString(), eq("Mis Entregas"), anyString()))
                    .thenReturn(Map.of("fileId", "alum-f", "webUrl", "https://od/alum"));

            entregaService.realizarEntrega(1L, 1L, "Entrega ambos", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            // Referencia del profesor tiene prioridad
            assertThat(mat.getOnedriveFileId()).isEqualTo("prof-f");
            assertThat(mat.getOnedriveOwnerId()).isEqualTo(10L);
            // Ambas subidas fueron invocadas
            verify(oneDriveService).subirArchivo(eq(10L), any(), anyString(),
                    anyString(), anyString(), eq("Alumno Test"), anyString());
            verify(oneDriveService).subirArchivo(eq(1L), any(), anyString(),
                    anyString(), anyString(), eq("Mis Entregas"), anyString());
        }

        @Test
        @DisplayName("Profesor falla → usa referencia del alumno")
        void profesorFallaUsaAlumno() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(true);
            when(oneDriveService.subirArchivo(eq(10L), any(), anyString(),
                    anyString(), anyString(), eq("Alumno Test"), anyString()))
                    .thenThrow(new RuntimeException("OneDrive profesor caído"));
            when(oneDriveService.subirArchivo(eq(1L), any(), anyString(),
                    anyString(), anyString(), eq("Mis Entregas"), anyString()))
                    .thenReturn(Map.of("fileId", "alum-fallback", "webUrl", "https://od/alum"));

            entregaService.realizarEntrega(1L, 1L, "Entrega fallback", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isEqualTo("alum-fallback");
            assertThat(mat.getOnedriveOwnerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Ambos fallan → cae a almacenamiento local")
        void ambosFallanFallbackLocal() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(true);
            when(oneDriveService.subirArchivo(eq(10L), any(), anyString(),
                    anyString(), anyString(), eq("Alumno Test"), anyString()))
                    .thenThrow(new RuntimeException("Error profesor"));
            when(oneDriveService.subirArchivo(eq(1L), any(), anyString(),
                    anyString(), anyString(), eq("Mis Entregas"), anyString()))
                    .thenThrow(new RuntimeException("Error alumno"));

            entregaService.realizarEntrega(1L, 1L, "Entrega local fallback", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isNull();
            assertThat(mat.getRuta()).doesNotStartWith("onedrive://");
        }

        @Test
        @DisplayName("OneDrive habilitado pero nadie conectado → almacenamiento local")
        void nadieConectadoFallbackLocal() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(false);
            when(oneDriveService.estaConectado(1L)).thenReturn(false);

            entregaService.realizarEntrega(1L, 1L, "Entrega sin OD", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isNull();
            assertThat(mat.getRuta()).doesNotStartWith("onedrive://");
            verify(oneDriveService, never()).subirArchivo(anyLong(), any(), anyString(),
                    anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("OneDrive deshabilitado → almacenamiento local directo")
        void oneDriveDeshabilitado() {
            actividad.setSubirAOneDrive(true);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(false);

            entregaService.realizarEntrega(1L, 1L, "Entrega sin servicio", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isNull();
            verify(oneDriveService, never()).estaConectado(anyLong());
        }

        @Test
        @DisplayName("Actividad sin subirAOneDrive → almacenamiento local directo")
        void actividadSinOneDrive() {
            actividad.setSubirAOneDrive(false);
            prepararMocksBaseRealizarEntrega();

            entregaService.realizarEntrega(1L, 1L, "Entrega local", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isNull();
            verify(oneDriveService, never()).isEnabled();
        }

        @Test
        @DisplayName("Profesor sin oneDriveUsuarioId → solo intenta alumno")
        void profesorSinOneDriveUsuarioId() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(null);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(true);
            when(oneDriveService.subirArchivo(eq(1L), any(), anyString(),
                    anyString(), anyString(), eq("Mis Entregas"), anyString()))
                    .thenReturn(Map.of("fileId", "alum-only", "webUrl", "https://od/alum"));

            entregaService.realizarEntrega(1L, 1L, "Entrega solo alumno", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getOnedriveFileId()).isEqualTo("alum-only");
            assertThat(mat.getOnedriveOwnerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Sube múltiples archivos a OneDrive")
        void multiplesArchivos() {
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);
            prepararMocksBaseRealizarEntrega();
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(false);
            when(oneDriveService.subirArchivo(eq(10L), any(), anyString(),
                    anyString(), anyString(), eq("Alumno Test"), anyString()))
                    .thenReturn(Map.of("fileId", "f1", "webUrl", "https://od/f1"))
                    .thenReturn(Map.of("fileId", "f2", "webUrl", "https://od/f2"));

            MockMultipartFile archivo1 = new MockMultipartFile(
                    "file", "doc1.pdf", "application/pdf", "pdf1".getBytes());
            MockMultipartFile archivo2 = new MockMultipartFile(
                    "file", "doc2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "docx2".getBytes());

            entregaService.realizarEntrega(1L, 1L, "Multi archivos", List.of(archivo1, archivo2));

            verify(materialRepository, times(2)).save(any(Material.class));
            verify(oneDriveService, times(2)).subirArchivo(eq(10L), any(), anyString(),
                    anyString(), anyString(), eq("Alumno Test"), anyString());
        }

        @Test
        @DisplayName("Archivo sin extensión genera nombre correcto")
        void archivoSinExtension() {
            actividad.setSubirAOneDrive(false);
            prepararMocksBaseRealizarEntrega();
            MockMultipartFile sinExt = new MockMultipartFile(
                    "file", "README", "text/plain", "contenido".getBytes());

            entregaService.realizarEntrega(1L, 1L, "Sin ext", List.of(sinExt));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getNombre()).isEqualTo("README");
        }

        @Test
        @DisplayName("Archivo con nombre null se maneja sin error")
        void archivoNombreNull() {
            actividad.setSubirAOneDrive(false);
            prepararMocksBaseRealizarEntrega();
            MockMultipartFile nullName = new MockMultipartFile(
                    "file", null, "application/octet-stream", "bytes".getBytes());

            entregaService.realizarEntrega(1L, 1L, "Null name", List.of(nullName));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            assertThat(captor.getValue().getRuta()).isNotNull();
        }
    }

    // =====================================================================
    // ALMACENAMIENTO LOCAL – guardarArchivoLocal
    // =====================================================================

    @Nested
    @DisplayName("Almacenamiento local")
    class AlmacenamientoLocal {

        @Test
        @DisplayName("Crea directorio y guarda archivo correctamente")
        void guardaArchivoLocalCreandoDirectorio(@TempDir Path tempDir) throws Exception {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            actividad.setSubirAOneDrive(false);
            prepararMocksBaseRealizarEntrega();

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "memoria.pdf", "application/pdf", "contenido-real".getBytes());

            entregaService.realizarEntrega(1L, 1L, "Entrega local", List.of(archivo));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            Material mat = captor.getValue();
            assertThat(mat.getRuta()).contains("entregas");
            assertThat(mat.getTamanoBytes()).isEqualTo("contenido-real".getBytes().length);
            assertThat(mat.getTipoMaterial()).isEqualTo(TipoMaterial.PDF);
            // Verificar que el archivo existe en disco
            assertThat(Files.exists(Path.of(mat.getRuta()))).isTrue();
            assertThat(Files.readAllBytes(Path.of(mat.getRuta()))).isEqualTo("contenido-real".getBytes());
        }

        @Test
        @DisplayName("Lanza excepción si la ruta de upload es inválida")
        void errorAlGuardarLocal() {
            // Ruta inexistente que no se puede crear (ni en Windows ni en Linux)
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir",
                    "Z:\\ruta\\inexistente\\nopuede");
            actividad.setSubirAOneDrive(false);

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L))
                    .thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(List.of());
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "data".getBytes());

            assertThatThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, "Fail", List.of(archivo)))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("TipoMaterial se asigna correctamente según contentType")
        void tipoMaterialPorContentType(@TempDir Path tempDir) {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            actividad.setSubirAOneDrive(false);
            prepararMocksBaseRealizarEntrega();

            MockMultipartFile archivoImg = new MockMultipartFile(
                    "file", "foto.png", "image/png", "img-data".getBytes());

            entregaService.realizarEntrega(1L, 1L, "Imagen", List.of(archivoImg));

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            assertThat(captor.getValue().getTipoMaterial()).isEqualTo(TipoMaterial.IMAGEN);
        }
    }

    // =====================================================================
    // REENVÍO CON OneDrive – eliminación de versiones anteriores
    // =====================================================================

    @Nested
    @DisplayName("Reenvío con OneDrive – eliminación y nueva subida")
    class ReenvioOneDrive {

        @Test
        @DisplayName("Reenvío elimina archivos OneDrive anteriores y sube nuevos")
        void reenvioEliminaYSube(@TempDir Path tempDir) {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);

            Material matAnterior = Material.builder()
                    .id(5L).nombre("old.pdf")
                    .onedriveFileId("od-old-1").onedriveOwnerId(10L)
                    .build();
            Entrega entregaAnterior = Entrega.builder()
                    .id(2L).version(1).esVersionActiva(true)
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(matAnterior)).feedbacks(new HashSet<>())
                    .build();

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L))
                    .thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(new ArrayList<>(List.of(entregaAnterior)));
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.estaConectado(10L)).thenReturn(true);
            when(oneDriveService.estaConectado(1L)).thenReturn(false);
            when(oneDriveService.subirArchivo(eq(10L), any(), anyString(),
                    anyString(), anyString(), eq("Alumno Test"), anyString()))
                    .thenReturn(Map.of("fileId", "od-new-1", "webUrl", "https://od/new"));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            MockMultipartFile nuevoArchivo = new MockMultipartFile(
                    "file", "nuevo.pdf", "application/pdf", "nuevo-contenido".getBytes());

            entregaService.realizarEntrega(1L, 1L, "Reenvío OD", List.of(nuevoArchivo));

            // Verificar eliminación del archivo anterior
            verify(oneDriveService).eliminarArchivo(10L, "od-old-1");
            // Verificar subida del nuevo
            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(captor.capture());
            assertThat(captor.getValue().getOnedriveFileId()).isEqualTo("od-new-1");
        }

        @Test
        @DisplayName("Error al eliminar archivo anterior no bloquea el reenvío")
        void reenvioErrorEliminarNoCritico(@TempDir Path tempDir) {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            actividad.setSubirAOneDrive(true);
            actividad.setOneDriveUsuarioId(10L);

            Material matAnterior = Material.builder()
                    .id(5L).nombre("old.pdf")
                    .onedriveFileId("od-old-err").onedriveOwnerId(10L)
                    .build();
            Entrega entregaAnterior = Entrega.builder()
                    .id(2L).version(1).esVersionActiva(true)
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(matAnterior)).feedbacks(new HashSet<>())
                    .build();

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L))
                    .thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(new ArrayList<>(List.of(entregaAnterior)));
            when(oneDriveService.isEnabled()).thenReturn(true);
            doThrow(new RuntimeException("Error API OneDrive"))
                    .when(oneDriveService).eliminarArchivo(10L, "od-old-err");
            when(oneDriveService.estaConectado(10L)).thenReturn(false);
            when(oneDriveService.estaConectado(1L)).thenReturn(false);
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            MockMultipartFile nuevoArchivo = new MockMultipartFile(
                    "file", "nuevo.pdf", "application/pdf", "data".getBytes());

            // No debe lanzar excepción
            assertThatNoException().isThrownBy(() ->
                    entregaService.realizarEntrega(1L, 1L, "Reenvío err", List.of(nuevoArchivo)));
        }

        @Test
        @DisplayName("Reenvío con múltiples entregas anteriores desactiva todas")
        void reenvioMultiplesAnteriores(@TempDir Path tempDir) {
            ReflectionTestUtils.setField(entregaService, "uploadBaseDir", tempDir.toString());
            actividad.setSubirAOneDrive(false);

            Entrega anterior1 = Entrega.builder()
                    .id(2L).version(1).esVersionActiva(true)
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();
            Entrega anterior2 = Entrega.builder()
                    .id(3L).version(2).esVersionActiva(true)
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
            when(estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(1L, 1L))
                    .thenReturn(Optional.of(estudiante));
            when(entregaRepository.findByEntregableIdAndEstudianteId(1L, 1L))
                    .thenReturn(new ArrayList<>(List.of(anterior1, anterior2)));
            when(entregaRepository.save(any(Entrega.class))).thenReturn(entrega);
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(mapper.toDTO(any(Entrega.class))).thenReturn(entregaDTO);

            entregaService.realizarEntrega(1L, 1L, "Versión 3", null);

            // Verifica que saveAll se llama con las anteriores desactivadas
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Entrega>> listCaptor = ArgumentCaptor.forClass(List.class);
            verify(entregaRepository).saveAll(listCaptor.capture());
            List<Entrega> desactivadas = listCaptor.getValue();
            assertThat(desactivadas).allMatch(e -> !e.getEsVersionActiva());
        }
    }

    // =====================================================================
    // DESCARGA DE ARCHIVOS CON CONTENIDO REAL
    // =====================================================================

    @Nested
    @DisplayName("Descarga de archivos con contenido real")
    class DescargaArchivosReales {

        @Test
        @DisplayName("descargarContenidoArchivo lee archivo local correctamente")
        void descargarLocalConContenidoReal(@TempDir Path tempDir) throws Exception {
            Path archivoReal = tempDir.resolve("real.txt");
            Files.write(archivoReal, "contenido verificable".getBytes());

            Material material = Material.builder()
                    .id(1L).nombre("real.txt")
                    .tipoMaterial(TipoMaterial.OTRO)
                    .ruta(archivoReal.toString())
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            byte[] result = entregaService.descargarContenidoArchivo(1L);

            assertThat(new String(result)).isEqualTo("contenido verificable");
        }

        @Test
        @DisplayName("descargarContenidoArchivo lanza error si archivo local no existe")
        void descargarLocalArchivoInexistente() {
            Material material = Material.builder()
                    .id(1L).nombre("inexistente.txt")
                    .ruta("Z:\\no\\existe\\archivo.txt")
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            assertThatThrownBy(() -> entregaService.descargarContenidoArchivo(1L))
                    .isInstanceOf(UncheckedIOException.class);
        }
    }

    // =====================================================================
    // GENERACIÓN DE ZIP CON CONTENIDO VERIFICABLE
    // =====================================================================

    @Nested
    @DisplayName("Generación ZIP con contenido verificable")
    class GeneracionZipReal {

        @Test
        @DisplayName("descargarTodoComoZip genera ZIP con estructura estudiante/archivo")
        void descargarTodoComoZipConContenido(@TempDir Path tempDir) throws Exception {
            Path archivoFisico = tempDir.resolve("tarea.pdf");
            Files.write(archivoFisico, "contenido-tarea".getBytes());

            Material material = Material.builder()
                    .id(10L).nombre("tarea.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta(archivoFisico.toString())
                    .build();

            Entrega entregaConArchivos = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(material)).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entregaConArchivos));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(material));

            byte[] zipBytes = entregaService.descargarTodoComoZip(1L);

            // Verificar estructura del ZIP
            Map<String, byte[]> entries = extractZipEntries(zipBytes);
            assertThat(entries).hasSize(1);
            String expectedEntry = "Alumno Test/tarea.pdf";
            assertThat(entries).containsKey(expectedEntry);
            assertThat(new String(entries.get(expectedEntry))).isEqualTo("contenido-tarea");
        }

        @Test
        @DisplayName("descargarTodoComoZip con múltiples estudiantes")
        void descargarTodoConMultiplesEstudiantes(@TempDir Path tempDir) throws Exception {
            Path archivo1 = tempDir.resolve("a1.txt");
            Path archivo2 = tempDir.resolve("a2.txt");
            Files.write(archivo1, "contenido1".getBytes());
            Files.write(archivo2, "contenido2".getBytes());

            Usuario usuario2 = Usuario.builder().id(2L).nombre("Estudiante 2")
                    .correoElectronico("est2@test.com").contrasena("pass").build();
            Estudiante estudiante2 = Estudiante.builder().id(2L).usuario(usuario2).grupo(grupo).build();

            Material mat1 = Material.builder().id(10L).nombre("a1.txt")
                    .tipoMaterial(TipoMaterial.OTRO).ruta(archivo1.toString()).build();
            Material mat2 = Material.builder().id(11L).nombre("a2.txt")
                    .tipoMaterial(TipoMaterial.OTRO).ruta(archivo2.toString()).build();

            Entrega entrega1 = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(mat1)).feedbacks(new HashSet<>()).build();
            Entrega entrega2 = Entrega.builder()
                    .id(2L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante2)
                    .archivos(Set.of(mat2)).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entrega1, entrega2));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(mat1));
            when(materialRepository.findById(11L)).thenReturn(Optional.of(mat2));

            byte[] zipBytes = entregaService.descargarTodoComoZip(1L);

            Map<String, byte[]> entries = extractZipEntries(zipBytes);
            assertThat(entries).hasSize(2);
            assertThat(entries).containsKey("Alumno Test/a1.txt");
            assertThat(entries).containsKey("Estudiante 2/a2.txt");
        }

        @Test
        @DisplayName("descargarTodoActividadComoZip genera estructura entregable/estudiante/archivo")
        void descargarTodoActividadConContenido(@TempDir Path tempDir) throws Exception {
            Path archivoFisico = tempDir.resolve("memoria.pdf");
            Files.write(archivoFisico, "memoria-contenido".getBytes());

            Material material = Material.builder()
                    .id(10L).nombre("memoria.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta(archivoFisico.toString())
                    .build();

            Entrega entregaConArchivos = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(material)).feedbacks(new HashSet<>()).build();

            when(entregableRepository.findByActividadId(1L)).thenReturn(List.of(entregable));
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entregaConArchivos));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(material));

            byte[] zipBytes = entregaService.descargarTodoActividadComoZip(1L);

            Map<String, byte[]> entries = extractZipEntries(zipBytes);
            assertThat(entries).hasSize(1);
            String expectedEntry = "Entregable 1/Alumno Test/memoria.pdf";
            assertThat(entries).containsKey(expectedEntry);
            assertThat(new String(entries.get(expectedEntry))).isEqualTo("memoria-contenido");
        }

        @Test
        @DisplayName("descargarTodoActividadComoZip con múltiples entregables")
        void descargarTodoActividadMultiplesEntregables(@TempDir Path tempDir) throws Exception {
            Path archivo1 = tempDir.resolve("e1.txt");
            Path archivo2 = tempDir.resolve("e2.txt");
            Files.write(archivo1, "data1".getBytes());
            Files.write(archivo2, "data2".getBytes());

            Entregable entregable2 = Entregable.builder()
                    .id(2L).titulo("Entregable 2").notaMaxima(10.0)
                    .permiteReenvio(true).actividad(actividad)
                    .entregas(new HashSet<>())
                    .fechaLimite(LocalDateTime.now().plusDays(7))
                    .build();

            Material mat1 = Material.builder().id(10L).nombre("e1.txt")
                    .tipoMaterial(TipoMaterial.OTRO).ruta(archivo1.toString()).build();
            Material mat2 = Material.builder().id(11L).nombre("e2.txt")
                    .tipoMaterial(TipoMaterial.OTRO).ruta(archivo2.toString()).build();

            Entrega entrega1 = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(mat1)).feedbacks(new HashSet<>()).build();
            Entrega entrega2 = Entrega.builder()
                    .id(2L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable2).estudiante(estudiante)
                    .archivos(Set.of(mat2)).feedbacks(new HashSet<>()).build();

            when(entregableRepository.findByActividadId(1L))
                    .thenReturn(List.of(entregable, entregable2));
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entrega1));
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(2L, true))
                    .thenReturn(List.of(entrega2));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(mat1));
            when(materialRepository.findById(11L)).thenReturn(Optional.of(mat2));

            byte[] zipBytes = entregaService.descargarTodoActividadComoZip(1L);

            Map<String, byte[]> entries = extractZipEntries(zipBytes);
            assertThat(entries).hasSize(2);
            assertThat(entries).containsKey("Entregable 1/Alumno Test/e1.txt");
            assertThat(entries).containsKey("Entregable 2/Alumno Test/e2.txt");
        }

        @Test
        @DisplayName("descargarTodoComoZip: archivo con error no bloquea los demás")
        void descargarTodoUnoFalla(@TempDir Path tempDir) throws Exception {
            Path archivoOk = tempDir.resolve("ok.txt");
            Files.write(archivoOk, "dato-ok".getBytes());

            Material matOk = Material.builder().id(10L).nombre("ok.txt")
                    .tipoMaterial(TipoMaterial.OTRO).ruta(archivoOk.toString()).build();
            Material matFail = Material.builder().id(11L).nombre("fail.txt")
                    .tipoMaterial(TipoMaterial.OTRO).ruta("Z:\\no\\existe.txt").build();

            Entrega entregaMixed = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(new LinkedHashSet<>(List.of(matOk, matFail)))
                    .feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entregaMixed));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(matOk));
            when(materialRepository.findById(11L)).thenReturn(Optional.of(matFail));

            byte[] zipBytes = entregaService.descargarTodoComoZip(1L);

            // Al menos el ZIP se genera (el archivo que falló se omite con warning)
            assertThat(zipBytes).isNotNull();
            assertThat(zipBytes.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("descargarTodoComoZip con archivos OneDrive")
        void descargarTodoDesdeOneDrive() {
            Material matOd = Material.builder()
                    .id(10L).nombre("cloud.pdf")
                    .tipoMaterial(TipoMaterial.PDF)
                    .ruta("onedrive://od-cloud-1")
                    .onedriveFileId("od-cloud-1")
                    .onedriveOwnerId(10L)
                    .build();

            Entrega entregaOd = Entrega.builder()
                    .id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true).entregable(entregable).estudiante(estudiante)
                    .archivos(Set.of(matOd)).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(entregaOd));
            when(materialRepository.findById(10L)).thenReturn(Optional.of(matOd));
            when(oneDriveService.descargarArchivo(10L, "od-cloud-1"))
                    .thenReturn("cloud-content".getBytes());

            byte[] zipBytes = entregaService.descargarTodoComoZip(1L);

            Map<String, byte[]> entries = extractZipEntries(zipBytes);
            assertThat(entries).containsKey("Alumno Test/cloud.pdf");
            assertThat(new String(entries.get("Alumno Test/cloud.pdf"))).isEqualTo("cloud-content");
        }
    }

    // =====================================================================
    // LISTAR CONTENIDO ZIP – CASOS ADICIONALES
    // =====================================================================

    @Nested
    @DisplayName("listarContenidoZip – casos adicionales")
    class ListarContenidoZipAdicional {

        @Test
        @DisplayName("ZIP con archivo local genera listado correctamente")
        void listarContenidoZipLocal(@TempDir Path tempDir) throws Exception {
            // Crear un ZIP real en disco
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("carpeta/"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("carpeta/archivo.java"));
                zos.write("public class Main {}".getBytes());
                zos.closeEntry();
            }

            Path zipFile = tempDir.resolve("entrega.zip");
            Files.write(zipFile, baos.toByteArray());

            Material material = Material.builder()
                    .id(1L).nombre("entrega.zip")
                    .tipoMaterial(TipoMaterial.ZIP)
                    .ruta(zipFile.toString())
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

            List<Map<String, Object>> result = entregaService.listarContenidoZip(1L);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(m ->
                    "carpeta/".equals(m.get("nombre")) && Boolean.TRUE.equals(m.get("esCarpeta")));
            assertThat(result).anyMatch(m ->
                    "carpeta/archivo.java".equals(m.get("nombre")) && Boolean.FALSE.equals(m.get("esCarpeta")));
        }

        @Test
        @DisplayName("ZIP vacío devuelve lista vacía")
        void listarContenidoZipVacio() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.finish();
            }

            Material material = Material.builder()
                    .id(1L).nombre("vacio.zip")
                    .tipoMaterial(TipoMaterial.ZIP)
                    .ruta(null)
                    .onedriveFileId("od-empty")
                    .onedriveOwnerId(10L)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(10L, "od-empty")).thenReturn(baos.toByteArray());

            List<Map<String, Object>> result = entregaService.listarContenidoZip(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ZIP con múltiples niveles de carpetas")
        void listarContenidoZipProfundo() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("src/"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("src/main/"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("src/main/java/"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("src/main/java/App.java"));
                zos.write("class App {}".getBytes());
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("README.md"));
                zos.write("# Info".getBytes());
                zos.closeEntry();
            }

            Material material = Material.builder()
                    .id(1L).nombre("proyecto.zip")
                    .tipoMaterial(TipoMaterial.ZIP)
                    .ruta(null)
                    .onedriveFileId("od-deep")
                    .onedriveOwnerId(10L)
                    .build();

            when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
            when(oneDriveService.descargarArchivo(10L, "od-deep")).thenReturn(baos.toByteArray());

            List<Map<String, Object>> result = entregaService.listarContenidoZip(1L);

            assertThat(result).hasSize(5);
            assertThat(result.stream().filter(m -> Boolean.TRUE.equals(m.get("esCarpeta"))).count())
                    .isEqualTo(3);
            assertThat(result.stream().filter(m -> Boolean.FALSE.equals(m.get("esCarpeta"))).count())
                    .isEqualTo(2);
        }
    }

    // =====================================================================
    // ESTADÍSTICAS – CASOS ADICIONALES
    // =====================================================================

    @Nested
    @DisplayName("Estadísticas – escenarios adicionales")
    class EstadisticasAdicionales {

        @Test
        @DisplayName("Promedio de calificación con múltiples entregas calificadas")
        void promedioConMultiplesCalificaciones() {
            Entrega e1 = Entrega.builder().id(1L).version(1).estado(EstadoEntrega.CALIFICADO)
                    .calificacion(7.0).esVersionActiva(true)
                    .fechaEntrega(LocalDateTime.of(2026, 1, 15, 10, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();
            Entrega e2 = Entrega.builder().id(2L).version(1).estado(EstadoEntrega.CALIFICADO)
                    .calificacion(9.0).esVersionActiva(true)
                    .fechaEntrega(LocalDateTime.of(2026, 1, 15, 10, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();
            Entrega e3 = Entrega.builder().id(3L).version(1).estado(EstadoEntrega.CALIFICADO)
                    .calificacion(8.0).esVersionActiva(true)
                    .fechaEntrega(LocalDateTime.of(2026, 1, 15, 10, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(e1, e2, e3));

            var result = entregaService.obtenerEstadisticas(1L);

            assertThat(result.getTotalEntregas()).isEqualTo(3);
            assertThat(result.getEntregasCalificadas()).isEqualTo(3);
            assertThat(result.getEntregasPendientes()).isZero();
            assertThat(result.getPromedioCalificacion()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Todas las entregas a tiempo")
        void todasATiempo() {
            Entrega e1 = Entrega.builder().id(1L).version(1).estado(EstadoEntrega.ENTREGADO)
                    .esVersionActiva(true)
                    .fechaEntrega(LocalDateTime.of(2026, 1, 1, 10, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            when(entregableRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.findByEntregableIdAndEsVersionActiva(1L, true))
                    .thenReturn(List.of(e1));

            var result = entregaService.obtenerEstadisticas(1L);

            assertThat(result.getEntregasATiempo()).isEqualTo(1);
            assertThat(result.getEntregasTardias()).isZero();
        }
    }

    // =====================================================================
    // LISTAR TODAS ENTREGAS ESTUDIANTE – ORDENACIÓN
    // =====================================================================

    @Nested
    @DisplayName("listarTodasEntregasEstudiante – ordenación y múltiples estudiantes")
    class ListarTodasAdicional {

        @Test
        @DisplayName("Ordena entregas por fecha descendente")
        void ordenaPorFechaDescendente() {
            Entrega e1 = Entrega.builder().id(1L).version(1)
                    .fechaEntrega(LocalDateTime.of(2026, 1, 10, 10, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();
            Entrega e2 = Entrega.builder().id(2L).version(1)
                    .fechaEntrega(LocalDateTime.of(2026, 2, 15, 10, 0))
                    .entregable(entregable).estudiante(estudiante)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            EntregaDTO dto1 = EntregaDTO.builder().id(1L).build();
            EntregaDTO dto2 = EntregaDTO.builder().id(2L).build();

            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(List.of(estudiante));
            when(entregaRepository.findByEstudianteId(1L)).thenReturn(List.of(e1, e2));
            when(mapper.toDTO(e2)).thenReturn(dto2);
            when(mapper.toDTO(e1)).thenReturn(dto1);

            List<EntregaDTO> result = entregaService.listarTodasEntregasEstudiante(1L);

            assertThat(result).hasSize(2);
            // La más reciente primero
            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(1).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Combina entregas de múltiples inscripciones")
        void multiplesInscripciones() {
            Curso curso2 = Curso.builder().id(2L).titulo("BD").codigo("BD-001").build();
            Grupo grupo2 = Grupo.builder().id(2L).titulo("G2").curso(curso2)
                    .estudiantes(new HashSet<>()).build();
            Estudiante est2 = Estudiante.builder().id(2L).usuario(usuario).grupo(grupo2).build();

            Entrega entOtroCurso = Entrega.builder().id(3L).version(1)
                    .fechaEntrega(LocalDateTime.of(2026, 3, 1, 10, 0))
                    .entregable(entregable).estudiante(est2)
                    .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

            EntregaDTO dto1 = EntregaDTO.builder().id(1L).build();
            EntregaDTO dto3 = EntregaDTO.builder().id(3L).build();

            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(List.of(estudiante, est2));
            when(entregaRepository.findByEstudianteId(1L)).thenReturn(List.of(entrega));
            when(entregaRepository.findByEstudianteId(2L)).thenReturn(List.of(entOtroCurso));
            when(mapper.toDTO(entrega)).thenReturn(dto1);
            when(mapper.toDTO(entOtroCurso)).thenReturn(dto3);

            List<EntregaDTO> result = entregaService.listarTodasEntregasEstudiante(1L);

            assertThat(result).hasSize(2);
        }
    }

    // =====================================================================
    // UTILIDADES
    // =====================================================================

    /** Extrae las entradas de un ZIP en un mapa nombre → contenido. */
    private static Map<String, byte[]> extractZipEntries(byte[] zipBytes) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zis.readAllBytes());
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al extraer ZIP en test", e);
        }
        return entries;
    }
}
