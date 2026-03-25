package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.Material;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.ActividadService;
import com.tfg.gestionentregables.service.EntregableService;
import com.tfg.gestionentregables.service.EntregaService;
import com.tfg.gestionentregables.service.SecurityContextUserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EntregaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EntregaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EntregaService entregaService;
    @MockitoBean private EntregableService entregableService;
    @MockitoBean private ActividadService actividadService;
        @MockitoBean private SecurityContextUserService securityContextUserService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private EntregaDTO entregaDTO;
    private EntregaResumenDTO entregaResumenDTO;
    private EntregaEstadisticasDTO estadisticasDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

                when(securityContextUserService.getCurrentUserId(any())).thenReturn(1L);
                when(securityContextUserService.hasRole(any(), eq("ADMIN"))).thenReturn(true);
                when(securityContextUserService.hasRole(any(), eq("PROFESOR"))).thenReturn(true);
                when(securityContextUserService.hasRole(any(), eq("ESTUDIANTE"))).thenReturn(false);

        entregaDTO = EntregaDTO.builder()
                .id(1L).nombre("Mi entrega").version(1)
                .fechaEntrega(LocalDateTime.now())
                .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                .entregableId(1L).entregableTitulo("E1")
                .estudianteId(1L).estudianteNombre("Alumno")
                .archivos(List.of()).feedbacks(List.of()).build();

        entregaResumenDTO = EntregaResumenDTO.builder()
                .entregaId(1L).estudianteId(1L).estudianteNombre("Alumno")
                .estado(EstadoEntrega.ENTREGADO).version(1).build();

        estadisticasDTO = EntregaEstadisticasDTO.builder()
                .entregableId(1L).totalEntregas(5L).entregasATiempo(4L)
                .entregasTardias(1L).entregasCalificadas(3L).entregasPendientes(2L)
                .promedioCalificacion(7.5).build();
    }

    @Nested
    @DisplayName("GET /api/entregas/{id}")
    class ObtenerEntrega {

        @Test
        @DisplayName("200 - Obtiene entrega")
        void obtener_ok() throws Exception {
            when(entregaService.obtenerEntrega(1L)).thenReturn(entregaDTO);

            mockMvc.perform(get("/api/entregas/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Mi entrega"));
        }

        @Test
        @DisplayName("404 - No encontrada")
        void obtener_notFound() throws Exception {
            when(entregaService.obtenerEntrega(99L))
                    .thenThrow(new EntityNotFoundException("No encontrada"));

            mockMvc.perform(get("/api/entregas/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/entregas/entregable/{entregableId}/estudiante/{estudianteId}")
    class RealizarEntrega {

        @Test
        @DisplayName("201 - Realiza entrega")
        void realizar_ok() throws Exception {
            when(entregaService.realizarEntrega(eq(1L), eq(1L), eq("Mi entrega"), isNull(), any()))
                    .thenReturn(entregaDTO);

            mockMvc.perform(post("/api/entregas/entregable/1/estudiante/1")
                            .param("nombre", "Mi entrega"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Mi entrega"));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/entregable/{entregableId}")
    class ListarParaEvaluar {

        @Test
        @DisplayName("200 - Lista entregas para evaluar")
        void listar_ok() throws Exception {
            when(entregaService.listarEntregasParaEvaluar(1L)).thenReturn(List.of(entregaResumenDTO));

            mockMvc.perform(get("/api/entregas/entregable/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/entregable/{entregableId}/estudiante/{estudianteId}")
    class ListarHistorial {

        @Test
        @DisplayName("200 - Lista historial de versiones")
        void listar_ok() throws Exception {
            when(entregaService.listarEntregasEstudiante(1L, 1L)).thenReturn(List.of(entregaDTO));

            mockMvc.perform(get("/api/entregas/entregable/1/estudiante/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/entregas/{id}/calificar")
    class CalificarEntrega {

        @Test
        @DisplayName("200 - Califica entrega")
        void calificar_ok() throws Exception {
            CalificacionDTO cal = CalificacionDTO.builder().nota(8.5).build();
                        when(entregaService.calificarEntrega(eq(1L), eq(1L), any())).thenReturn(entregaDTO);

            mockMvc.perform(post("/api/entregas/1/calificar")
                                                        .param("profesorId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cal)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 - Nota supera máxima")
        void calificar_notaInvalida() throws Exception {
            CalificacionDTO cal = CalificacionDTO.builder().nota(15.0).build();
            when(entregaService.calificarEntrega(eq(1L), eq(1L), any()))
                    .thenThrow(new IllegalArgumentException("La calificación no puede ser mayor"));

            mockMvc.perform(post("/api/entregas/1/calificar")
                            .param("profesorId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cal)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/estudiante/{estudianteId}")
    class ListarTodasEstudiante {

        @Test
        @DisplayName("200 - Lista todas entregas del estudiante")
        void listar_ok() throws Exception {
            when(entregaService.listarTodasEntregasEstudiante(1L)).thenReturn(List.of(entregaDTO));

            mockMvc.perform(get("/api/entregas/estudiante/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/profesor/{profesorId}/pendientes")
    class ListarPendientes {

        @Test
        @DisplayName("200 - Lista pendientes de calificar")
        void listar_ok() throws Exception {
            when(entregaService.listarEntregasPendientesCalificar(1L))
                    .thenReturn(List.of(entregaResumenDTO));

            mockMvc.perform(get("/api/entregas/profesor/1/pendientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/entregable/{entregableId}/estadisticas")
    class Estadisticas {

        @Test
        @DisplayName("200 - Obtiene estadísticas")
        void obtener_ok() throws Exception {
            when(entregaService.obtenerEstadisticas(1L)).thenReturn(estadisticasDTO);

            mockMvc.perform(get("/api/entregas/entregable/1/estadisticas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEntregas").value(5));
        }
    }

    @Nested
    @DisplayName("DELETE /api/entregas/{id}")
    class EliminarEntrega {

        @Test
        @DisplayName("204 - Elimina entrega")
        void eliminar_ok() throws Exception {
            doNothing().when(entregaService).eliminarEntrega(1L);

            mockMvc.perform(delete("/api/entregas/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("409 - Entrega calificada no se puede eliminar")
        void eliminar_calificada() throws Exception {
            doThrow(new IllegalStateException("No se puede eliminar una entrega ya calificada"))
                    .when(entregaService).eliminarEntrega(1L);

            mockMvc.perform(delete("/api/entregas/1"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/archivo/{materialId}/preview")
    class PrevisualizarArchivo {

        @Test
        @DisplayName("200 - Previsualiza PDF con content-type correcto")
        void preview_pdf() throws Exception {
            Material material = Material.builder()
                    .id(1L).nombre("informe.pdf").tipoMaterial(TipoMaterial.PDF).build();
            byte[] contenido = "contenido-pdf".getBytes();

            when(entregaService.obtenerArchivo(1L)).thenReturn(material);
            when(entregaService.descargarContenidoArchivo(1L)).thenReturn(contenido);

            mockMvc.perform(get("/api/entregas/archivo/1/preview"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "inline; filename=\"informe.pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(contenido));
        }

        @Test
        @DisplayName("200 - Previsualiza imagen PNG")
        void preview_png() throws Exception {
            Material material = Material.builder()
                    .id(2L).nombre("diagrama.png").tipoMaterial(TipoMaterial.IMAGEN).build();
            byte[] contenido = "contenido-png".getBytes();

            when(entregaService.obtenerArchivo(2L)).thenReturn(material);
            when(entregaService.descargarContenidoArchivo(2L)).thenReturn(contenido);

            mockMvc.perform(get("/api/entregas/archivo/2/preview"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG));
        }

        @Test
        @DisplayName("200 - Previsualiza archivo TXT")
        void preview_txt() throws Exception {
            Material material = Material.builder()
                    .id(3L).nombre("readme.txt").tipoMaterial(TipoMaterial.TXT).build();
            byte[] contenido = "Hola mundo".getBytes();

            when(entregaService.obtenerArchivo(3L)).thenReturn(material);
            when(entregaService.descargarContenidoArchivo(3L)).thenReturn(contenido);

            mockMvc.perform(get("/api/entregas/archivo/3/preview"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.TEXT_PLAIN));
        }

        @Test
        @DisplayName("200 - Archivo sin extensión conocida usa octet-stream")
        void preview_desconocido() throws Exception {
            Material material = Material.builder()
                    .id(4L).nombre("archivo.xyz").tipoMaterial(TipoMaterial.OTRO).build();
            byte[] contenido = "datos".getBytes();

            when(entregaService.obtenerArchivo(4L)).thenReturn(material);
            when(entregaService.descargarContenidoArchivo(4L)).thenReturn(contenido);

            mockMvc.perform(get("/api/entregas/archivo/4/preview"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        }

        @Test
        @DisplayName("404 - Material no encontrado")
        void preview_notFound() throws Exception {
            when(entregaService.obtenerArchivo(99L))
                    .thenThrow(new EntityNotFoundException("No encontrado"));

            mockMvc.perform(get("/api/entregas/archivo/99/preview"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404 - Error al descargar contenido")
        void preview_errorDescarga() throws Exception {
            Material material = Material.builder()
                    .id(1L).nombre("archivo.pdf").tipoMaterial(TipoMaterial.PDF).build();

            when(entregaService.obtenerArchivo(1L)).thenReturn(material);
            when(entregaService.descargarContenidoArchivo(1L))
                    .thenThrow(new RuntimeException("Error de descarga"));

            mockMvc.perform(get("/api/entregas/archivo/1/preview"))
                    .andExpect(status().isNotFound());
        }
    }

        @Nested
        @DisplayName("GET /api/entregas/archivo/{materialId}")
        class DescargarArchivo {

                @Test
                @DisplayName("200 - Descarga archivo desde servicio")
                void descargar_ok() throws Exception {
                        Material material = Material.builder()
                                        .id(1L).nombre("entrega.pdf").ruta("onedrive://abc").tipoMaterial(TipoMaterial.PDF).build();
                        byte[] contenido = "contenido".getBytes();

                        when(entregaService.obtenerArchivo(1L)).thenReturn(material);
                        when(entregaService.descargarContenidoArchivo(1L)).thenReturn(contenido);

                        mockMvc.perform(get("/api/entregas/archivo/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                                        .andExpect(header().string("Content-Disposition", "attachment; filename=\"entrega.pdf\""))
                                        .andExpect(content().bytes(contenido));
                }

                @Test
                @DisplayName("200 - Fallback a ruta local cuando falla descarga remota")
                void descargar_fallbackLocal() throws Exception {
                        Path tempFile = Files.createTempFile("entrega-controller-", ".txt");
                        byte[] esperado = "local-file-content".getBytes();
                        Files.write(tempFile, esperado);

                        Material material = Material.builder()
                                        .id(2L).nombre("local.txt").ruta(tempFile.toString()).tipoMaterial(TipoMaterial.TXT).build();

                        when(entregaService.obtenerArchivo(2L)).thenReturn(material);
                        when(entregaService.descargarContenidoArchivo(2L)).thenThrow(new RuntimeException("Falla remota"));

                        try {
                                mockMvc.perform(get("/api/entregas/archivo/2"))
                                                .andExpect(status().isOk())
                                                .andExpect(content().bytes(esperado));
                        } finally {
                                Files.deleteIfExists(tempFile);
                        }
                }

                @Test
                @DisplayName("404 - Sin fallback local disponible")
                void descargar_notFound() throws Exception {
                        Material material = Material.builder()
                                        .id(3L).nombre("missing.bin").ruta(null).tipoMaterial(TipoMaterial.OTRO).build();

                        when(entregaService.obtenerArchivo(3L)).thenReturn(material);
                        when(entregaService.descargarContenidoArchivo(3L)).thenThrow(new RuntimeException("Falla"));

                        mockMvc.perform(get("/api/entregas/archivo/3"))
                                        .andExpect(status().isNotFound());
                }
        }

        @Nested
        @DisplayName("GET /api/entregas/actividad/{actividadId}/descargar-todo")
        class DescargarTodoActividad {

                @Test
                @DisplayName("200 - Descarga ZIP de actividad")
                void descargarTodoActividad_ok() throws Exception {
                        byte[] zipBytes = "zip-actividad".getBytes();
                        ActividadDTO actividad = ActividadDTO.builder().id(1L).titulo("Actividad 1").build();

                        when(actividadService.obtenerActividadPorId(1L)).thenReturn(actividad);
                        when(entregaService.descargarTodoActividadComoZip(1L)).thenReturn(zipBytes);

                        mockMvc.perform(get("/api/entregas/actividad/1/descargar-todo"))
                                        .andExpect(status().isOk())
                                        .andExpect(header().string("Content-Disposition", "attachment; filename=\"Actividad 1.zip\""))
                                        .andExpect(content().contentType("application/zip"))
                                        .andExpect(content().bytes(zipBytes));
                }

                @Test
                @DisplayName("404 - Error al generar ZIP de actividad")
                void descargarTodoActividad_error() throws Exception {
                        when(actividadService.obtenerActividadPorId(1L)).thenThrow(new EntityNotFoundException("No encontrada"));

                        mockMvc.perform(get("/api/entregas/actividad/1/descargar-todo"))
                                        .andExpect(status().isNotFound());
                }
        }

        @Nested
        @DisplayName("GET /api/entregas/archivo/{materialId}/zip-contenido")
        class ListarContenidoZip {

                @Test
                @DisplayName("200 - Lista contenido interno del ZIP")
                void listarContenido_ok() throws Exception {
                        List<Map<String, Object>> contenido = List.of(
                                        Map.of("nombre", "docs/readme.txt", "esDirectorio", false, "tamano", 12L));

                        when(entregaService.listarContenidoZip(1L)).thenReturn(contenido);

                        mockMvc.perform(get("/api/entregas/archivo/1/zip-contenido"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$[0].nombre").value("docs/readme.txt"));
                }

                @Test
                @DisplayName("404 - Error listando contenido ZIP")
                void listarContenido_error() throws Exception {
                        when(entregaService.listarContenidoZip(1L)).thenThrow(new RuntimeException("ZIP inválido"));

                        mockMvc.perform(get("/api/entregas/archivo/1/zip-contenido"))
                                        .andExpect(status().isNotFound());
                }
        }

    @Nested
    @DisplayName("GET /api/entregas/entregable/{entregableId}/descargar-todo")
    class DescargarTodo {

        @Test
        @DisplayName("200 - Descarga ZIP con todas las entregas")
        void descargarTodo_ok() throws Exception {
            byte[] zipBytes = "fake-zip-content".getBytes();
            EntregableDTO entregableDTO = EntregableDTO.builder().id(1L).titulo("Entregable Test").build();
            when(entregableService.obtenerEntregable(1L)).thenReturn(entregableDTO);
            when(entregaService.descargarTodoComoZip(1L)).thenReturn(zipBytes);

            mockMvc.perform(get("/api/entregas/entregable/1/descargar-todo"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"Entregable Test.zip\""))
                    .andExpect(content().contentType("application/zip"))
                    .andExpect(content().bytes(zipBytes));
        }

        @Test
        @DisplayName("404 - Entregable no encontrado")
        void descargarTodo_notFound() throws Exception {
            when(entregableService.obtenerEntregable(99L))
                    .thenThrow(new EntityNotFoundException("No encontrado"));

            mockMvc.perform(get("/api/entregas/entregable/99/descargar-todo"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404 - Error al generar ZIP")
        void descargarTodo_error() throws Exception {
            EntregableDTO entregableDTO = EntregableDTO.builder().id(1L).titulo("Entregable Test").build();
            when(entregableService.obtenerEntregable(1L)).thenReturn(entregableDTO);
            when(entregaService.descargarTodoComoZip(1L))
                    .thenThrow(new RuntimeException("Error interno"));

            mockMvc.perform(get("/api/entregas/entregable/1/descargar-todo"))
                    .andExpect(status().isNotFound());
        }
    }
}
