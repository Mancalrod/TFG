package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.CalificacionDTO;
import com.tfg.gestionentregables.dto.EntregaDTO;
import com.tfg.gestionentregables.dto.EntregaEstadisticasDTO;
import com.tfg.gestionentregables.dto.EntregaPendienteDTO;
import com.tfg.gestionentregables.dto.EntregaResumenDTO;
import com.tfg.gestionentregables.entity.Actividad;
import com.tfg.gestionentregables.entity.Curso;
import com.tfg.gestionentregables.entity.Entregable;
import com.tfg.gestionentregables.entity.Entrega;
import com.tfg.gestionentregables.entity.Estudiante;
import com.tfg.gestionentregables.entity.Feedback;
import com.tfg.gestionentregables.entity.Material;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.repository.EntregableRepository;
import com.tfg.gestionentregables.repository.EntregaRepository;
import com.tfg.gestionentregables.repository.ActividadRepository;
import com.tfg.gestionentregables.repository.EstudianteRepository;
import com.tfg.gestionentregables.repository.FeedbackRepository;
import com.tfg.gestionentregables.repository.MaterialRepository;
import com.tfg.gestionentregables.repository.ProfesorRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EntregaService {

    private static final String ENTREGA_NOT_FOUND = "Entrega no encontrada con ID: ";
    private static final int MAX_ARCHIVOS_POR_ENTREGA = 50;
    private static final String ONEDRIVE_SCHEME = "onedrive://";
    private static final String CLOUDINARY_SCHEME = "cloudinary://";
    private static final String ONE_DRIVE_STUDENT_BASE_FOLDER = "TFG-Entregables/Mis Entregas";
    private static final char ZIP_SEPARATOR = '/';

    private final EntregaRepository entregaRepository;
    private final EntregableRepository entregableRepository;
    private final ActividadRepository actividadRepository;
    private final EstudianteRepository estudianteRepository;
    private final MaterialRepository materialRepository;
    private final FeedbackRepository feedbackRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProfesorRepository profesorRepository;
    private final EntityMapper mapper;
    private final OneDriveService oneDriveService;
    private final ZipValidationService zipValidationService;
    private final CloudinaryService cloudinaryService;

    @Value("${app.upload.dir:uploads}")
    private String uploadBaseDir;

    private record EntregaEntrada(String nombre,
                                  String comentarioNormalizado,
                                  List<MultipartFile> archivos,
                                  boolean tieneComentario,
                                  boolean tieneArchivos) {
    }

    private record OneDriveMetadata(String fileId, String webUrl, Long ownerId) {
        private boolean disponible() {
            return fileId != null;
        }
    }

    public EntregaDTO realizarEntrega(Long entregableId,
                                      Long usuarioId,
                                      String nombre,
                                      String comentario,
                                      List<MultipartFile> archivos) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId));

        Long cursoId = entregable.getActividad().getCurso().getId();
        Estudiante estudiante = estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(usuarioId, cursoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estudiante no encontrado para el usuario con ID: " + usuarioId + " en el curso correspondiente"));

        EntregaEntrada entrada = validarYPrepararEntrada(entregable, nombre, comentario, archivos);

        List<Entrega> entregasAnteriores = entregaRepository
                .findByEntregableIdAndEstudianteId(entregableId, estudiante.getId());
        int ultimaVersion = entregasAnteriores.stream().mapToInt(Entrega::getVersion).max().orElse(0);

        if (ultimaVersion > 0 && !Boolean.TRUE.equals(entregable.getPermiteReenvio())) {
            throw new IllegalStateException("Este entregable no permite reenvío de entregas");
        }

        desactivarVersionesAnteriores(entregable.getActividad(), entregasAnteriores);

        int nuevaVersion = ultimaVersion + 1;
        String nombreEntrega = !entrada.nombre().isBlank()
            ? entrada.nombre()
                : "Entrega " + entregable.getTitulo() + " v" + nuevaVersion;

        Entrega entrega = Entrega.builder()
                .nombre(nombreEntrega)
                .comentarioAlumno(entrada.tieneComentario() ? entrada.comentarioNormalizado() : null)
                .version(nuevaVersion)
                .fechaEntrega(LocalDateTime.now())
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregable)
                .estudiante(estudiante)
                .build();

        entrega = entregaRepository.save(entrega);

        if (entrada.tieneArchivos()) {
            guardarArchivosAdjuntos(entrada.archivos(), entrega, entregable, estudiante);
        }

        Long entregaId = entrega.getId();
        Entrega entregaActualizada = entregaRepository.findById(entregaId)
            .orElseThrow(() -> new EntityNotFoundException(ENTREGA_NOT_FOUND + entregaId));
        return mapper.toDTO(entregaActualizada);
    }

    /**
     * Compatibilidad con llamadas anteriores que no enviaban comentario.
     */
    public EntregaDTO realizarEntrega(Long entregableId,
                                      Long usuarioId,
                                      String nombre,
                                      List<MultipartFile> archivos) {
        return realizarEntrega(entregableId, usuarioId, nombre, null, archivos);
    }

    private EntregaEntrada validarYPrepararEntrada(Entregable entregable,
                                                   String nombre,
                                                   String comentario,
                                                   List<MultipartFile> archivos) {
        String nombreNormalizado = nombre == null ? "" : nombre.trim();
        String comentarioNormalizado = comentario == null ? "" : comentario.trim();
        List<MultipartFile> archivosNormalizados = archivos == null ? List.of() : archivos;

        boolean tieneComentario = !comentarioNormalizado.isEmpty();
        boolean tieneArchivos = !archivosNormalizados.isEmpty();

        validarContenidoEntrega(entregable, nombreNormalizado, comentarioNormalizado, tieneComentario, tieneArchivos, archivosNormalizados);
        validarArchivosAdjuntos(entregable, archivosNormalizados);

        return new EntregaEntrada(nombreNormalizado, comentarioNormalizado, archivosNormalizados, tieneComentario, tieneArchivos);
    }

    private void validarContenidoEntrega(Entregable entregable,
                                         String nombreNormalizado,
                                         String comentarioNormalizado,
                                         boolean tieneComentario,
                                         boolean tieneArchivos,
                                         List<MultipartFile> archivosNormalizados) {
        TipoMaterial tipoEsperado = entregable.getTipoArchivoEsperado();
        boolean esSoloTexto = tipoEsperado == TipoMaterial.SOLO_TEXTO;
        boolean permiteSoloComentario = permiteEntregaSoloComentario(tipoEsperado);

        if (!tieneComentario && !tieneArchivos && nombreNormalizado.isBlank()) {
            throw new IllegalArgumentException("Debes adjuntar al menos un archivo o escribir un comentario");
        }

        if (esSoloTexto && tieneArchivos) {
            throw new IllegalArgumentException("Este entregable es de solo texto: no se permiten archivos adjuntos");
        }
        if (esSoloTexto && !tieneComentario) {
            throw new IllegalArgumentException("Este entregable es de solo texto: debes escribir un comentario");
        }
        if (!esSoloTexto && !tieneArchivos && tieneComentario && !permiteSoloComentario) {
            throw new IllegalArgumentException(
                    "Para este tipo de entregable debes adjuntar al menos un archivo; el comentario es opcional pero no sustituye al archivo");
        }
        if (tieneComentario && comentarioNormalizado.length() > 5000) {
            throw new IllegalArgumentException("El comentario no puede exceder 5000 caracteres");
        }
        if (tieneArchivos && archivosNormalizados.size() > MAX_ARCHIVOS_POR_ENTREGA) {
            throw new IllegalArgumentException("No se pueden adjuntar más de " + MAX_ARCHIVOS_POR_ENTREGA + " archivos en una entrega");
        }
    }

    private void guardarArchivosAdjuntos(List<MultipartFile> archivos,
                                         Entrega entrega,
                                         Entregable entregable,
                                         Estudiante estudiante) {
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                throw new IllegalArgumentException("No se permiten archivos vacíos en la entrega");
            }
            Material material = guardarArchivoConOneDrive(archivo, entrega, entregable, estudiante);
            materialRepository.save(material);
        }
    }

    @Transactional(readOnly = true)
    public EntregaDTO obtenerEntrega(Long entregaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException(ENTREGA_NOT_FOUND + entregaId));
        return mapper.toDTO(entrega);
    }

    @Transactional(readOnly = true)
    public List<EntregaResumenDTO> listarEntregasParaEvaluar(Long entregableId) {
        if (!entregableRepository.existsById(entregableId)) {
            throw new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId);
        }

        return entregaRepository.findByEntregableIdAndEsVersionActiva(entregableId, true)
                .stream()
                .map(mapper::toResumenDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntregaDTO> listarEntregasEstudiante(Long entregableId, Long usuarioId) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId));

        Long cursoId = entregable.getActividad().getCurso().getId();
        Estudiante estudiante = estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(usuarioId, cursoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estudiante no encontrado para el usuario con ID: " + usuarioId + " en el curso correspondiente"));

        return entregaRepository.findByEntregableIdAndEstudianteId(entregableId, estudiante.getId())
                .stream()
                .sorted((a, b) -> Integer.compare(b.getVersion(), a.getVersion()))
                .map(mapper::toDTO)
                .toList();
    }

    public EntregaDTO calificarEntrega(Long entregaId, Long profesorId, CalificacionDTO calificacion) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException("Entrega no encontrada con ID: " + entregaId));

        Long cursoId = entrega.getEntregable().getActividad().getCurso().getId();
        if (profesorId != null && profesorId > 0) {
            boolean esProfesorDelCurso = profesorRepository.existsByUsuarioIdAndCursoId(profesorId, cursoId);
            if (!esProfesorDelCurso) {
                throw new AccessDeniedException("No tienes permisos para calificar entregas de este curso");
            }
        }

        if (calificacion.getNota() == null) {
            throw new IllegalArgumentException("La nota es obligatoria");
        }
        if (calificacion.getNota() < 0) {
            throw new IllegalArgumentException("La nota no puede ser negativa");
        }

        Double notaMaxima = entrega.getEntregable().getNotaMaxima();
        if (notaMaxima != null && calificacion.getNota() > notaMaxima) {
            throw new IllegalArgumentException("La calificación no puede ser mayor que la nota máxima (" + notaMaxima + ")");
        }

        entrega.setCalificacion(calificacion.getNota());
        entrega.setEstado(EstadoEntrega.CALIFICADO);
        entrega.setFechaCalificacion(LocalDateTime.now());
        entrega = entregaRepository.save(entrega);

        if (profesorId != null && profesorId > 0
            && calificacion.getComentario() != null
            && !calificacion.getComentario().trim().isEmpty()) {
            Usuario profesor = usuarioRepository.findById(profesorId)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + profesorId));

            Feedback feedback = Feedback.builder()
                    .comentario(calificacion.getComentario().trim())
                    .fechaCreacion(LocalDateTime.now())
                    .fechaModificacion(LocalDateTime.now())
                    .entrega(entrega)
                    .profesor(profesor)
                    .build();
            feedbackRepository.save(feedback);
        }

        Entrega entregaActualizada = entregaRepository.findById(entrega.getId()).orElse(entrega);
        return mapper.toDTO(entregaActualizada);
    }

    /**
     * Compatibilidad con llamadas anteriores sin profesorId.
     */
    public EntregaDTO calificarEntrega(Long entregaId, CalificacionDTO calificacion) {
        return calificarEntrega(entregaId, -1L, calificacion);
    }

    @Transactional(readOnly = true)
    public Material obtenerArchivo(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + materialId));
    }

    @Transactional(readOnly = true)
    public List<EntregaDTO> listarTodasEntregasEstudiante(Long usuarioId) {
        List<Estudiante> estudiantes = estudianteRepository.findByUsuarioId(usuarioId);
        if (estudiantes.isEmpty()) {
            throw new EntityNotFoundException("Estudiante no encontrado para el usuario con ID: " + usuarioId);
        }

        return estudiantes.stream()
                .flatMap(est -> entregaRepository.findByEstudianteId(est.getId()).stream())
                .sorted((a, b) -> b.getFechaEntrega().compareTo(a.getFechaEntrega()))
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntregaResumenDTO> listarEntregasPendientesCalificar(Long profesorId) {
        return entregaRepository.findByEstadoAndEsVersionActiva(EstadoEntrega.ENTREGADO, true)
                .stream()
            .filter(e -> profesorRepository.existsByUsuarioIdAndCursoId(
                profesorId,
                e.getEntregable().getActividad().getCurso().getId()))
                .map(mapper::toResumenDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntregaPendienteDTO> listarPendientesEstudiante(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        List<Estudiante> matriculas = estudianteRepository.findByUsuarioId(usuarioId);

        return matriculas.stream()
            .flatMap(estudiante -> pendientesPorMatricula(estudiante, ahora).stream())
            .sorted(Comparator.comparing(EntregaPendienteDTO::getFechaLimite,
                Comparator.nullsLast(LocalDateTime::compareTo)))
            .toList();
    }

    private List<EntregaPendienteDTO> pendientesPorMatricula(Estudiante estudiante, LocalDateTime ahora) {
        Long grupoId = estudiante.getGrupo().getId();
        return actividadRepository.findByGrupoId(grupoId).stream()
            .filter(actividad -> perteneceActividadAGrupo(actividad, estudiante))
            .flatMap(actividad -> actividad.getEntregables().stream()
                .filter(entregable -> entregableVisibleYVigente(entregable, ahora))
                .filter(entregable -> !tieneEntregaActiva(entregable, estudiante))
                .map(entregable -> construirPendienteDTO(estudiante, actividad, entregable, ahora)))
            .toList();
    }

    private boolean perteneceActividadAGrupo(Actividad actividad, Estudiante estudiante) {
        return actividad.getGrupos().stream()
            .anyMatch(grupo -> grupo.getId().equals(estudiante.getGrupo().getId()));
    }

    private boolean entregableVisibleYVigente(Entregable entregable, LocalDateTime ahora) {
        return entregable.getVisibilidad() == Visibilidad.VISIBLE
            && (entregable.getFechaInicio() == null || !entregable.getFechaInicio().isAfter(ahora))
            && (entregable.getFechaLimite() == null || entregable.getFechaLimite().isAfter(ahora));
    }

    private boolean tieneEntregaActiva(Entregable entregable, Estudiante estudiante) {
        return entregaRepository
            .findByEntregableIdAndEstudianteIdAndEsVersionActivaTrue(entregable.getId(), estudiante.getId())
            .isPresent();
    }

    private EntregaPendienteDTO construirPendienteDTO(Estudiante estudiante,
                                                      Actividad actividad,
                                                      Entregable entregable,
                                                      LocalDateTime ahora) {
        Curso curso = estudiante.getGrupo().getCurso();
        LocalDateTime fechaLimite = entregable.getFechaLimite();
        return EntregaPendienteDTO.builder()
            .cursoId(curso.getId())
            .cursoTitulo(curso.getTitulo())
            .actividadId(actividad.getId())
            .actividadTitulo(actividad.getTitulo())
            .entregableId(entregable.getId())
            .entregableTitulo(entregable.getTitulo())
            .fechaLimite(fechaLimite)
            .tiempoRestante(formatearTiempoRestante(ahora, fechaLimite))
            .build();
    }

    private String formatearTiempoRestante(LocalDateTime ahora, LocalDateTime fechaLimite) {
        if (fechaLimite == null) {
            return "Sin límite";
        }
        long totalHoras = ChronoUnit.HOURS.between(ahora, fechaLimite);
        if (totalHoras <= 0) {
            return "Expirado";
        }
        long dias = totalHoras / 24;
        long horas = totalHoras % 24;
        if (dias == 0) {
            return horas + "h";
        }
        return dias + "d " + horas + "h";
        }

    @Transactional(readOnly = true)
    public EntregaEstadisticasDTO obtenerEstadisticas(Long entregableId) {
        if (!entregableRepository.existsById(entregableId)) {
            throw new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId);
        }

        List<Entrega> entregasActivas = entregaRepository.findByEntregableIdAndEsVersionActiva(entregableId, true);

        long totalEntregas = entregasActivas.size();
        long entregasATiempo = entregasActivas.stream().filter(Entrega::fueATiempo).count();
        long entregasCalificadas = entregasActivas.stream()
                .filter(e -> e.getEstado() == EstadoEntrega.CALIFICADO || e.getEstado() == EstadoEntrega.PUBLICADO)
                .count();
        Double promedio = entregasActivas.stream()
                .filter(e -> e.getCalificacion() != null)
                .mapToDouble(Entrega::getCalificacion)
                .average()
                .orElse(0.0);

        return EntregaEstadisticasDTO.builder()
                .entregableId(entregableId)
                .totalEntregas(totalEntregas)
                .entregasATiempo(entregasATiempo)
                .entregasTardias(totalEntregas - entregasATiempo)
                .entregasCalificadas(entregasCalificadas)
                .entregasPendientes(totalEntregas - entregasCalificadas)
                .promedioCalificacion(promedio > 0 ? promedio : null)
                .build();
    }

    public void eliminarEntrega(Long entregaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException("Entrega no encontrada con ID: " + entregaId));

        if (entrega.getEstado() == EstadoEntrega.CALIFICADO || entrega.getEstado() == EstadoEntrega.PUBLICADO) {
            throw new IllegalStateException("No se puede eliminar una entrega ya calificada");
        }

        entregaRepository.delete(entrega);
    }

    public byte[] descargarContenidoArchivo(Long materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + materialId));

        if (material.getOnedriveFileId() != null && material.getOnedriveOwnerId() != null) {
            try {
                return oneDriveService.descargarArchivo(material.getOnedriveOwnerId(), material.getOnedriveFileId());
            } catch (Exception e) {
                log.error("Error al descargar desde OneDrive, intentando fallback local: {}", e.getMessage());
                if (material.getRuta() != null && !material.getRuta().startsWith(ONEDRIVE_SCHEME)) {
                    return leerArchivoLocal(material.getRuta());
                }
                throw new IllegalStateException("No se pudo descargar el archivo de OneDrive", e);
            }
        }

        if (material.getCloudinaryPublicId() != null && material.getCloudinaryUrl() != null) {
            try {
                return cloudinaryService.descargarArchivo(material.getCloudinaryUrl());
            } catch (Exception e) {
                log.error("Error al descargar desde Cloudinary: {}", e.getMessage());
                throw new IllegalStateException("No se pudo descargar el archivo de Cloudinary", e);
            }
        }

        if (material.getRuta() != null) {
            return leerArchivoLocal(material.getRuta());
        }

        throw new IllegalStateException("El archivo no tiene ruta de almacenamiento válida");
    }

    @Transactional(readOnly = true)
    public byte[] descargarTodoComoZip(Long entregableId) {
        if (!entregableRepository.existsById(entregableId)) {
            throw new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId);
        }

        List<Entrega> entregasActivas = entregaRepository.findByEntregableIdAndEsVersionActiva(entregableId, true);
        return construirZipEntregas(entregasActivas, null);
    }

    @Transactional(readOnly = true)
    public byte[] descargarTodoActividadComoZip(Long actividadId) {
        List<Entregable> entregables = entregableRepository.findByActividadId(actividadId);
        if (entregables.isEmpty()) {
            throw new EntityNotFoundException("No se encontraron entregables para la actividad con ID: " + actividadId);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> usedEntries = new HashSet<>();

            for (Entregable entregable : entregables) {
                String carpetaEntregable = sanitizarSegmentoZip(entregable.getTitulo());
                List<Entrega> entregas = entregaRepository.findByEntregableIdAndEsVersionActiva(entregable.getId(), true);
                incluirEntregasEnZip(zos, entregas, carpetaEntregable, usedEntries);
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al crear el ZIP de entregas de la actividad", e);
        }
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("java:S5042")
    public List<Map<String, Object>> listarContenidoZip(Long materialId) {
        byte[] contenido = descargarContenidoArchivo(materialId);
        List<Map<String, Object>> entradas = new ArrayList<>();
        final int maxEntries = 10_000;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entradas.size() >= maxEntries) {
                    throw new IOException("ZIP con demasiadas entradas (máximo " + maxEntries + ")");
                }
                if (esEntradaZipPeligrosa(entry.getName())) {
                    throw new IOException("Entrada ZIP con ruta no permitida: " + entry.getName());
                }

                Map<String, Object> item = new HashMap<>();
                item.put("nombre", entry.getName());
                item.put("tamano", entry.getSize() >= 0 ? entry.getSize() : 0);
                item.put("esCarpeta", entry.isDirectory());
                entradas.add(item);
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error al leer contenido del ZIP", e);
        }

        return entradas;
    }

    private boolean permiteEntregaSoloComentario(TipoMaterial tipoEsperado) {
        return tipoEsperado == null
                || tipoEsperado == TipoMaterial.OTRO
                || tipoEsperado == TipoMaterial.ENLACE
                || tipoEsperado == TipoMaterial.SOLO_TEXTO;
    }

    private void validarArchivosAdjuntos(Entregable entregable, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        boolean requiereZipEstructurado = requiereZipEstructurado(entregable);
        validarArchivoUnicoZipSiAplica(archivos, requiereZipEstructurado);

        for (MultipartFile archivo : archivos) {
            validarArchivoNoVacio(archivo);
            validarZipEstructurado(entregable, archivo, requiereZipEstructurado);
        }
    }

    private boolean requiereZipEstructurado(Entregable entregable) {
        boolean tieneEstructura = entregable.getEstructuraZip() != null && !entregable.getEstructuraZip().isBlank();
        boolean tieneNombreZipEsperado = entregable.getNombreZipEsperado() != null
                && !entregable.getNombreZipEsperado().isBlank()
                && !"*".equals(entregable.getNombreZipEsperado().trim());
        return tieneEstructura || tieneNombreZipEsperado;
    }

    private void validarArchivoUnicoZipSiAplica(List<MultipartFile> archivos, boolean requiereZipEstructurado) {
        if (!requiereZipEstructurado) {
            return;
        }
        if (archivos.size() != 1) {
            throw new IllegalArgumentException("Este entregable requiere adjuntar un único archivo ZIP");
        }

        MultipartFile unicoArchivo = archivos.get(0);
        String nombreUnico = unicoArchivo != null ? unicoArchivo.getOriginalFilename() : null;
        if (nombreUnico == null || !nombreUnico.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Este entregable requiere adjuntar un único archivo con extensión .zip");
        }
    }

    private void validarArchivoNoVacio(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No se permiten archivos vacíos en la entrega");
        }
    }

    private void validarZipEstructurado(Entregable entregable, MultipartFile archivo, boolean requiereZipEstructurado) {
        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            nombreArchivo = "archivo_sin_nombre";
        }
        if (!requiereZipEstructurado || !nombreArchivo.toLowerCase().endsWith(".zip")) {
            return;
        }

        boolean estricta = Boolean.TRUE.equals(entregable.getValidacionZipEstricta());
        ZipValidationService.ResultadoValidacion resultado = zipValidationService.validarZip(
                archivo,
                entregable.getEstructuraZip(),
                estricta,
                entregable.getNombreZipEsperado());

        if (!resultado.valido()) {
            throw new IllegalStateException("El archivo ZIP no cumple la estructura esperada:\n"
                    + String.join("\n", resultado.errores()));
        }
    }

    private void desactivarVersionesAnteriores(Actividad actividad, List<Entrega> entregasAnteriores) {
        if (Boolean.TRUE.equals(actividad.getSubirAOneDrive()) && oneDriveService.isEnabled()) {
            entregasAnteriores.stream()
                    .filter(entregaAnterior -> Boolean.TRUE.equals(entregaAnterior.getEsVersionActiva()))
                    .forEach(this::eliminarArchivosOneDriveEntrega);
        }

        entregasAnteriores.forEach(e -> e.setEsVersionActiva(false));
        if (!entregasAnteriores.isEmpty()) {
            entregaRepository.saveAll(entregasAnteriores);
        }
    }

    private void eliminarArchivosOneDriveEntrega(Entrega entregaAnterior) {
        for (Material mat : entregaAnterior.getArchivos()) {
            if (mat.getOnedriveFileId() == null || mat.getOnedriveOwnerId() == null) {
                continue;
            }
            try {
                oneDriveService.eliminarArchivo(mat.getOnedriveOwnerId(), mat.getOnedriveFileId());
            } catch (Exception e) {
                log.warn("No se pudo eliminar archivo OneDrive al reenviar: {}", e.getMessage());
            }
        }
    }

    private Material guardarArchivoConOneDrive(MultipartFile archivo,
                                               Entrega entrega,
                                               Entregable entregable,
                                               Estudiante estudiante) {
        String nombreOriginal = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename()
                : "archivo_sin_nombre";
        log.info("Subiendo archivo - nombreOriginal recibido: '{}', size: {}", nombreOriginal, archivo.getSize());
        String extension = nombreOriginal.contains(".")
                ? nombreOriginal.substring(nombreOriginal.lastIndexOf("."))
                : "";
        String nombreUnico = UUID.randomUUID() + extension;

        Actividad actividad = entregable.getActividad();

        if (Boolean.TRUE.equals(actividad.getSubirAOneDrive()) && oneDriveService.isEnabled()) {
            return guardarEnOneDrive(archivo, entrega, entregable, estudiante, nombreOriginal);
        }

        if (cloudinaryService.isEnabled()) {
            return guardarEnCloudinary(archivo, entrega, entregable, nombreOriginal);
        }

        return guardarArchivoLocal(archivo, entrega, nombreOriginal, nombreUnico);
    }

    private Material guardarEnOneDrive(MultipartFile archivo,
                                       Entrega entrega,
                                       Entregable entregable,
                                       Estudiante estudiante,
                                       String nombreOriginal) {
        Actividad actividad = entregable.getActividad();
        Curso curso = actividad.getCurso();
        String nombreArchivo = nombreOriginal;
        String cursoTitulo = curso.getTitulo();
        String actividadTitulo = actividad.getTitulo();
        String entregableTitulo = entregable.getTitulo();
        String estudianteNombre = estudiante.getUsuario().getNombre();

        OneDriveMetadata metadataProfesor = subirAlOneDriveProfesor(
                archivo,
                actividad,
                cursoTitulo,
                actividadTitulo,
                entregableTitulo,
                estudianteNombre,
                nombreArchivo);
        OneDriveMetadata metadataAlumno = subirAlOneDriveAlumno(
                archivo,
                entrega,
            nombreArchivo,
            estudiante,
            entregable);

        OneDriveMetadata metadataFinal = metadataProfesor.disponible() ? metadataProfesor : metadataAlumno;
        if (metadataFinal.disponible()) {
            return Material.builder()
                    .nombre(nombreOriginal)
                    .tipoMaterial(determinarTipoMaterial(archivo.getContentType()))
                    .ruta(ONEDRIVE_SCHEME + metadataFinal.fileId())
                    .tamanoBytes(archivo.getSize())
                    .onedriveFileId(metadataFinal.fileId())
                    .onedriveWebUrl(metadataFinal.webUrl())
                    .onedriveOwnerId(metadataFinal.ownerId())
                    .entrega(entrega)
                    .build();
        }

        return guardarArchivoLocal(archivo, entrega, nombreOriginal,
                UUID.randomUUID() + (nombreOriginal.contains(".") ? nombreOriginal.substring(nombreOriginal.lastIndexOf(".")) : ""));
    }

    private OneDriveMetadata subirAlOneDriveProfesor(MultipartFile archivo,
                                                     Actividad actividad,
                                                     String cursoTitulo,
                                                     String actividadTitulo,
                                                     String entregableTitulo,
                                                     String estudianteNombre,
                                                     String nombreArchivo) {
        Long profesorUsuarioId = actividad.getOneDriveUsuarioId();
        if (profesorUsuarioId == null || !oneDriveService.estaConectado(profesorUsuarioId)) {
            return new OneDriveMetadata(null, null, null);
        }

        try {
            Map<String, String> result = actividad.getCarpetaOneDrive() != null
                && !actividad.getCarpetaOneDrive().isBlank()
                ? oneDriveService.subirArchivo(
                    profesorUsuarioId,
                    archivo,
                    cursoTitulo,
                    actividadTitulo,
                    entregableTitulo,
                    estudianteNombre,
                    nombreArchivo,
                    actividad.getCarpetaOneDrive())
                : oneDriveService.subirArchivo(
                    profesorUsuarioId,
                    archivo,
                    cursoTitulo,
                    actividadTitulo,
                    entregableTitulo,
                    estudianteNombre,
                    nombreArchivo);

            return new OneDriveMetadata(result.get("fileId"), result.get("webUrl"), profesorUsuarioId);
        } catch (Exception e) {
            log.warn("Error al subir al OneDrive del profesor {}: {}", profesorUsuarioId, e.getMessage());
            return new OneDriveMetadata(null, null, null);
        }
    }

    private OneDriveMetadata subirAlOneDriveAlumno(MultipartFile archivo,
                                                   Entrega entrega,
                                                   String nombreArchivo,
                                                   Estudiante estudiante,
                                                   Entregable entregable) {
        Long estudianteUsuarioId = estudiante.getUsuario().getId();
        String estudianteNombre = estudiante.getUsuario().getNombre();
        if (!oneDriveService.estaConectado(estudianteUsuarioId)) {
            return new OneDriveMetadata(null, null, null);
        }

        try {
            Actividad actividad = entregable.getActividad();
            Curso curso = actividad.getCurso();
            String carpetaVersionAlumno = String.format(
                "%s/%s/%s/%s/v%d",
                ONE_DRIVE_STUDENT_BASE_FOLDER,
                curso.getTitulo(),
                actividad.getTitulo(),
                entregable.getTitulo(),
                entrega.getVersion());
            Map<String, String> alumnoResult = oneDriveService.subirArchivoEnRuta(
                estudianteUsuarioId,
                archivo,
                carpetaVersionAlumno,
                nombreArchivo);
            return new OneDriveMetadata(alumnoResult.get("fileId"), alumnoResult.get("webUrl"), estudianteUsuarioId);
        } catch (Exception e) {
            log.warn("Error al subir al OneDrive del alumno {}: {}", estudianteNombre, e.getMessage());
            return new OneDriveMetadata(null, null, null);
        }
    }

    private Material guardarArchivoLocal(MultipartFile archivo,
                                         Entrega entrega,
                                         String nombreOriginal,
                                         String nombreArchivo) {
        try {
            Path uploadPath = Paths.get(uploadBaseDir, "entregas", String.valueOf(entrega.getId())).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path rutaArchivo = uploadPath.resolve(nombreArchivo).normalize();
            if (!rutaArchivo.startsWith(uploadPath)) {
                throw new IllegalArgumentException("Ruta de archivo no válida");
            }

            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            return Material.builder()
                    .nombre(nombreOriginal)
                    .tipoMaterial(determinarTipoMaterial(archivo.getContentType()))
                    .ruta(rutaArchivo.toString())
                    .tamanoBytes(archivo.getSize())
                    .entrega(entrega)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al guardar el archivo: " + e.getMessage(), e);
        }
    }

    private Material guardarEnCloudinary(MultipartFile archivo,
                                         Entrega entrega,
                                         Entregable entregable,
                                         String nombreOriginal) {
        String carpeta = entregable.getActividad().getCurso().getCodigo() + "/"
                + entregable.getActividad().getTitulo() + "/"
                + entregable.getTitulo();

        Map<String, String> result = cloudinaryService.subirArchivo(archivo, carpeta);

        return Material.builder()
                .nombre(nombreOriginal)
                .tipoMaterial(determinarTipoMaterial(archivo.getContentType()))
            .ruta(CLOUDINARY_SCHEME + result.get("publicId"))
                .tamanoBytes(archivo.getSize())
                .cloudinaryPublicId(result.get("publicId"))
                .cloudinaryUrl(result.get("secureUrl"))
                .entrega(entrega)
                .build();
    }

    private byte[] construirZipEntregas(List<Entrega> entregas, String prefijoBase) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> usedEntries = new HashSet<>();
            incluirEntregasEnZip(zos, entregas, prefijoBase, usedEntries);

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al crear el ZIP de entregas", e);
        }
    }

    private void incluirEntregasEnZip(ZipOutputStream zos,
                                      List<Entrega> entregas,
                                      String prefijoBase,
                                      Set<String> usedEntries) throws IOException {
        for (Entrega entrega : entregas) {
            String carpetaEstudiante = sanitizarSegmentoZip(entrega.getEstudiante().getUsuario().getNombre());

            for (Material material : entrega.getArchivos()) {
                String nombreArchivo = sanitizarSegmentoZip(material.getNombre());
                String basePath = (prefijoBase == null || prefijoBase.isBlank())
                        ? unirSegmentosZip(carpetaEstudiante, nombreArchivo)
                        : unirSegmentosZip(prefijoBase, carpetaEstudiante, nombreArchivo);

                String entryName = evitarColisionZip(basePath, usedEntries);

                try {
                    byte[] contenido = descargarContenidoArchivo(material.getId());
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(contenido);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("No se pudo incluir archivo {} en el ZIP: {}", entryName, e.getMessage());
                }
            }
        }
    }

    private byte[] leerArchivoLocal(String ruta) {
        try {
            Path target = Paths.get(ruta).toAbsolutePath().normalize();

            if (uploadBaseDir == null || uploadBaseDir.isBlank()) {
                return Files.readAllBytes(target);
            }

            Path base = Paths.get(uploadBaseDir).toAbsolutePath().normalize();

            if (!target.startsWith(base) && !Files.exists(target)) {
                throw new IllegalArgumentException("Ruta local fuera del directorio de subida");
            }

            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Error al leer archivo local: " + e.getMessage(), e);
        }
    }

    private String sanitizarSegmentoZip(String input) {
        if (input == null || input.isBlank()) {
            return "sin_nombre";
        }
        String limpio = input.replace('\\', '_').replace('/', '_');
        limpio = limpio.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ._-]", "_").trim();
        return limpio.isEmpty() ? "sin_nombre" : limpio;
    }

    private String evitarColisionZip(String entryName, Set<String> usedEntries) {
        if (usedEntries.add(entryName)) {
            return entryName;
        }

        int dot = entryName.lastIndexOf('.');
        String base = dot > 0 ? entryName.substring(0, dot) : entryName;
        String ext = dot > 0 ? entryName.substring(dot) : "";

        int i = 2;
        String candidate;
        do {
            candidate = base + " (" + i + ")" + ext;
            i++;
        } while (!usedEntries.add(candidate));

        return candidate;
    }

    private String unirSegmentosZip(String... segmentos) {
        return String.join(String.valueOf(ZIP_SEPARATOR), segmentos);
    }

    private boolean esEntradaZipPeligrosa(String name) {
        return name == null || name.contains("..") || name.indexOf('/') == 0 || name.indexOf('\\') == 0;
    }

    private TipoMaterial determinarTipoMaterial(String contentType) {
        if (contentType == null) {
            return TipoMaterial.OTRO;
        }
        if (contentType.startsWith("application/pdf")) {
            return TipoMaterial.PDF;
        }
        if (contentType.startsWith("image/")) {
            return TipoMaterial.IMAGEN;
        }
        if (contentType.contains("zip") || contentType.contains("rar") || contentType.contains("7z")) {
            return TipoMaterial.ZIP;
        }
        if (contentType.contains("word") || contentType.contains("document")) {
            return TipoMaterial.DOCX;
        }
        return TipoMaterial.OTRO;
    }
}
