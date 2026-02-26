package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Servicio para gestión de feedback.
 * Implementa operaciones SYSOP-019 a SYSOP-021.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private static final String FEEDBACK_NOT_FOUND = "Feedback no encontrado con ID: ";

    private final FeedbackRepository feedbackRepository;
    private final EntregaRepository entregaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityMapper mapper;

    /**
     * SYSOP-019: Añade feedback a una entrega.
     */
    public FeedbackDTO crearFeedback(Long entregaId, Long profesorId, CrearFeedbackDTO dto) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException("Entrega no encontrada con ID: " + entregaId));
        
        Usuario profesor = usuarioRepository.findById(profesorId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + profesorId));

        LocalDateTime ahora = LocalDateTime.now();
        
        Feedback feedback = Feedback.builder()
                .comentario(dto.getComentario())
                .fechaCreacion(ahora)
                .fechaModificacion(ahora)
                .entrega(entrega)
                .profesor(profesor)
                .build();

        feedback = feedbackRepository.save(feedback);
        return mapper.toDTO(feedback);
    }

    /**
     * SYSOP-020: Lista feedbacks de una entrega.
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> listarFeedbacksEntrega(Long entregaId) {
        if (!entregaRepository.existsById(entregaId)) {
            throw new EntityNotFoundException("Entrega no encontrada con ID: " + entregaId);
        }
        
        return feedbackRepository.findByEntregaIdOrderByFechaCreacionDesc(entregaId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * SYSOP-021: Modifica un feedback existente.
     */
    public FeedbackDTO actualizarFeedback(Long feedbackId, Long profesorId, CrearFeedbackDTO dto) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + feedbackId));

        // Verificar que el profesor que modifica es el mismo que creó el feedback
        if (!feedback.getProfesor().getId().equals(profesorId)) {
            throw new IllegalStateException("Solo el profesor que creó el feedback puede modificarlo");
        }

        feedback.setComentario(dto.getComentario());
        feedback.setFechaModificacion(LocalDateTime.now());

        feedback = feedbackRepository.save(feedback);
        return mapper.toDTO(feedback);
    }

    /**
     * Obtiene un feedback por su ID.
     */
    @Transactional(readOnly = true)
    public FeedbackDTO obtenerFeedback(Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException(FEEDBACK_NOT_FOUND + feedbackId));
        return mapper.toDTO(feedback);
    }

    /**
     * Elimina un feedback.
     */
    public void eliminarFeedback(Long feedbackId, Long profesorId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException("Feedback no encontrado con ID: " + feedbackId));

        // Verificar que el profesor que elimina es el mismo que creó el feedback
        if (!feedback.getProfesor().getId().equals(profesorId)) {
            throw new IllegalStateException("Solo el profesor que creó el feedback puede eliminarlo");
        }

        feedbackRepository.delete(feedback);
    }

    /**
     * Lista todos los feedbacks dados por un profesor.
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> listarFeedbacksProfesor(Long profesorId) {
        if (!usuarioRepository.existsById(profesorId)) {
            throw new EntityNotFoundException("Usuario no encontrado con ID: " + profesorId);
        }
        
        return feedbackRepository.findByProfesorIdOrderByFechaCreacionDesc(profesorId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Cuenta feedbacks no leídos de un estudiante (entregas sin ver).
     */
    @Transactional(readOnly = true)
    public long contarFeedbacksRecientes(Long estudianteId, LocalDateTime desde) {
        return feedbackRepository.countFeedbacksRecientesParaEstudiante(estudianteId, desde);
    }
}
