package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ActividadTest {

    @Nested
    @DisplayName("estaEnPlazo()")
    class EstaEnPlazo {

        @Test
        @DisplayName("En plazo cuando la fecha actual está entre inicio y límite")
        void enPlazo_entreInicioYLimite() {
            Actividad actividad = Actividad.builder()
                    .fechaInicio(LocalDateTime.now().minusDays(5))
                    .fechaLimite(LocalDateTime.now().plusDays(5))
                    .build();

            assertThat(actividad.estaEnPlazo()).isTrue();
        }

        @Test
        @DisplayName("En plazo cuando fechaInicio es null")
        void enPlazo_sinFechaInicio() {
            Actividad actividad = Actividad.builder()
                    .fechaInicio(null)
                    .fechaLimite(LocalDateTime.now().plusDays(5))
                    .build();

            assertThat(actividad.estaEnPlazo()).isTrue();
        }

        @Test
        @DisplayName("Fuera de plazo cuando la fecha límite ya pasó")
        void fueraDePlazo_fechaLimitePasada() {
            Actividad actividad = Actividad.builder()
                    .fechaInicio(LocalDateTime.now().minusDays(10))
                    .fechaLimite(LocalDateTime.now().minusDays(1))
                    .build();

            assertThat(actividad.estaEnPlazo()).isFalse();
        }

        @Test
        @DisplayName("Fuera de plazo cuando la fecha de inicio es futura")
        void fueraDePlazo_fechaInicioFutura() {
            Actividad actividad = Actividad.builder()
                    .fechaInicio(LocalDateTime.now().plusDays(5))
                    .fechaLimite(LocalDateTime.now().plusDays(10))
                    .build();

            assertThat(actividad.estaEnPlazo()).isFalse();
        }
    }

    @Nested
    @DisplayName("esVisibleParaAlumnos()")
    class EsVisibleParaAlumnos {

        @Test
        @DisplayName("Visible cuando visibilidad es VISIBLE")
        void visible() {
            Actividad actividad = Actividad.builder()
                    .visibilidad(Visibilidad.VISIBLE)
                    .fechaLimite(LocalDateTime.now().plusDays(1))
                    .tipoActividad(TipoActividad.EVALUABLE)
                    .build();

            assertThat(actividad.esVisibleParaAlumnos()).isTrue();
        }

        @Test
        @DisplayName("No visible cuando visibilidad es OCULTO")
        void oculto() {
            Actividad actividad = Actividad.builder()
                    .visibilidad(Visibilidad.OCULTO)
                    .fechaLimite(LocalDateTime.now().plusDays(1))
                    .tipoActividad(TipoActividad.EVALUABLE)
                    .build();

            assertThat(actividad.esVisibleParaAlumnos()).isFalse();
        }
    }
}
