package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EntregaTest {

    private Entregable entregable;

    @BeforeEach
    void setUp() {
        entregable = Entregable.builder()
                .id(1L)
                .titulo("Entregable Test")
                .fechaInicio(LocalDateTime.now().minusDays(10))
                .fechaLimite(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Nested
    @DisplayName("fueATiempo()")
    class FueATiempo {

        @Test
        @DisplayName("Fue a tiempo cuando la entrega es antes de la fecha límite")
        void aTiempo_antesDeLimit() {
            Entrega entrega = Entrega.builder()
                    .fechaEntrega(LocalDateTime.now().minusDays(1))
                    .entregable(entregable)
                    .build();

            assertThat(entrega.fueATiempo()).isTrue();
        }

        @Test
        @DisplayName("Fue a tiempo cuando la entrega es exactamente en la fecha límite")
        void aTiempo_exactoEnLimite() {
            LocalDateTime limite = LocalDateTime.of(2026, 6, 15, 23, 59);
            entregable.setFechaLimite(limite);

            Entrega entrega = Entrega.builder()
                    .fechaEntrega(limite)
                    .entregable(entregable)
                    .build();

            assertThat(entrega.fueATiempo()).isTrue();
        }

        @Test
        @DisplayName("No fue a tiempo cuando la entrega es después de la fecha límite")
        void tarde_despuesDeLimite() {
            LocalDateTime limite = LocalDateTime.of(2026, 3, 1, 23, 59);
            entregable.setFechaLimite(limite);

            Entrega entrega = Entrega.builder()
                    .fechaEntrega(limite.plusMinutes(1))
                    .entregable(entregable)
                    .build();

            assertThat(entrega.fueATiempo()).isFalse();
        }
    }

    @Nested
    @DisplayName("calificar()")
    class Calificar {

        @Test
        @DisplayName("Calificar establece nota, fecha y estado CALIFICADO")
        void calificar_correctamente() {
            Entrega entrega = Entrega.builder()
                    .estado(EstadoEntrega.ENTREGADO)
                    .entregable(entregable)
                    .build();

            entrega.calificar(8.5);

            assertThat(entrega.getCalificacion()).isEqualTo(8.5);
            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.CALIFICADO);
            assertThat(entrega.getFechaCalificacion()).isNotNull();
        }

        @Test
        @DisplayName("Calificar con 0 también funciona")
        void calificar_conCero() {
            Entrega entrega = Entrega.builder()
                    .estado(EstadoEntrega.ENTREGADO)
                    .entregable(entregable)
                    .build();

            entrega.calificar(0.0);

            assertThat(entrega.getCalificacion()).isEqualTo(0.0);
            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.CALIFICADO);
        }
    }

    @Nested
    @DisplayName("publicarNota()")
    class PublicarNota {

        @Test
        @DisplayName("Publicar nota cambia estado a PUBLICADO")
        void publicar_correctamente() {
            Entrega entrega = Entrega.builder()
                    .estado(EstadoEntrega.CALIFICADO)
                    .calificacion(7.0)
                    .entregable(entregable)
                    .build();

            entrega.publicarNota();

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.PUBLICADO);
        }
    }
}
