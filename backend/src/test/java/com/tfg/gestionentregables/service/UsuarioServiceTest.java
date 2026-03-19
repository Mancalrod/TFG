package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private EntityMapper mapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private UsuarioDTO usuarioDTO;
    private CrearUsuarioDTO crearUsuarioDTO;
    private Profesor profesor;
    private Estudiante estudiante;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Juan")
                .telefono("123456").correoElectronico("juan@test.com")
                .contrasena("encoded").esAdmin(false).build();

        usuarioDTO = UsuarioDTO.builder().id(1L).nombre("Juan")
                .correoElectronico("juan@test.com").esAdmin(false).build();

        crearUsuarioDTO = CrearUsuarioDTO.builder()
                .nombre("Juan").telefono("123456")
                .correoElectronico("juan@test.com")
                .contrasena("password123").esAdmin(false).build();

        Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001").build();
        grupo = Grupo.builder().id(1L).titulo("G1").curso(curso).estudiantes(new HashSet<>()).build();
        profesor = Profesor.builder().id(1L).usuario(usuario).build();
        estudiante = Estudiante.builder().id(1L).usuario(usuario).grupo(grupo).build();
    }

    @Nested
    @DisplayName("obtenerUsuarioPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("Obtiene usuario existente")
        void obtener_ok() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(mapper.toDTO(usuario)).thenReturn(usuarioDTO);

            UsuarioDTO result = usuarioService.obtenerUsuarioPorId(1L);

            assertThat(result.getNombre()).isEqualTo("Juan");
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void obtener_noExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obtenerUsuarioPorId(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("obtenerUsuarioPorCorreo")
    class ObtenerPorCorreo {

        @Test
        @DisplayName("Obtiene usuario por correo")
        void obtener_ok() {
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));
            when(mapper.toDTO(usuario)).thenReturn(usuarioDTO);

            UsuarioDTO result = usuarioService.obtenerUsuarioPorCorreo("juan@test.com");

            assertThat(result.getCorreoElectronico()).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("Lanza excepción si correo no existe")
        void obtener_noExiste() {
            when(usuarioRepository.findByCorreoElectronico("x@x.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obtenerUsuarioPorCorreo("x@x.com"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarUsuarios")
    class ListarUsuarios {

        @Test
        @DisplayName("Lista todos los usuarios")
        void listar_ok() {
            when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
            when(mapper.toDTO(usuario)).thenReturn(usuarioDTO);

            List<UsuarioDTO> result = usuarioService.listarUsuarios();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devuelve lista vacía")
        void listar_vacia() {
            when(usuarioRepository.findAll()).thenReturn(List.of());

            List<UsuarioDTO> result = usuarioService.listarUsuarios();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("crearUsuario")
    class CrearUsuario {

        @Test
        @DisplayName("Crea usuario correctamente")
        void crear_ok() {
            when(usuarioRepository.existsByCorreoElectronico("juan@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(mapper.toDTO(any(Usuario.class))).thenReturn(usuarioDTO);

            UsuarioDTO result = usuarioService.crearUsuario(crearUsuarioDTO);

            assertThat(result.getNombre()).isEqualTo("Juan");
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Lanza excepción si correo duplicado")
        void crear_correoDuplicado() {
            when(usuarioRepository.existsByCorreoElectronico("juan@test.com")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crearUsuario(crearUsuarioDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("correo electrónico");
        }

        @Test
        @DisplayName("Asigna esAdmin false por defecto")
        void crear_adminDefault() {
            CrearUsuarioDTO dto = CrearUsuarioDTO.builder()
                    .nombre("X").correoElectronico("x@x.com").contrasena("pass")
                    .esAdmin(null).build();

            when(usuarioRepository.existsByCorreoElectronico("x@x.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("enc");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(mapper.toDTO(any(Usuario.class))).thenReturn(usuarioDTO);

            usuarioService.crearUsuario(dto);

            verify(usuarioRepository).save(argThat(u -> !u.getEsAdmin()));
        }
    }

    @Nested
    @DisplayName("actualizarUsuario")
    class ActualizarUsuario {

        @Test
        @DisplayName("Actualiza usuario correctamente")
        void actualizar_ok() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(mapper.toDTO(any(Usuario.class))).thenReturn(usuarioDTO);

            UsuarioDTO result = usuarioService.actualizarUsuario(1L, crearUsuarioDTO);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Lanza excepción si correo cambiado y ya existe")
        void actualizar_correoDuplicado() {
            CrearUsuarioDTO dto = CrearUsuarioDTO.builder()
                    .nombre("Juan").correoElectronico("otro@test.com")
                    .contrasena("pass").build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.existsByCorreoElectronico("otro@test.com")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.actualizarUsuario(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("No codifica contraseña si está vacía")
        void actualizar_sinContrasena() {
            CrearUsuarioDTO dto = CrearUsuarioDTO.builder()
                    .nombre("Juan").correoElectronico("juan@test.com")
                    .contrasena("").build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(mapper.toDTO(any(Usuario.class))).thenReturn(usuarioDTO);

            usuarioService.actualizarUsuario(1L, dto);

            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("Lanza excepción si usuario no existe")
        void actualizar_noExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.actualizarUsuario(99L, crearUsuarioDTO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("eliminarUsuario")
    class EliminarUsuario {

        @Test
        @DisplayName("Elimina usuario existente")
        void eliminar_ok() {
            when(usuarioRepository.existsById(1L)).thenReturn(true);

            usuarioService.eliminarUsuario(1L);

            verify(usuarioRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void eliminar_noExiste() {
            when(usuarioRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.eliminarUsuario(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("registrarComoProfesor")
    class RegistrarProfesor {

        @Test
        @DisplayName("Registra como profesor correctamente")
        void registrar_ok() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(false);

            usuarioService.registrarComoProfesor(1L);

            verify(profesorRepository).save(any(Profesor.class));
        }

        @Test
        @DisplayName("Bloquea si ya es profesor")
        void registrar_yaEsProfesor() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.registrarComoProfesor(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya está registrado como profesor");
        }

        @Test
        @DisplayName("Lanza excepción si usuario no existe")
        void registrar_noExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.registrarComoProfesor(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("registrarComoEstudiante")
    class RegistrarEstudiante {

        @Test
        @DisplayName("Registra como estudiante correctamente")
        void registrar_ok() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(grupoRepository.findById(1L)).thenReturn(Optional.of(grupo));
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(false);

            usuarioService.registrarComoEstudiante(1L, 1L);

            verify(estudianteRepository).save(any(Estudiante.class));
        }

        @Test
        @DisplayName("Bloquea si ya es estudiante")
        void registrar_yaEsEstudiante() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(grupoRepository.findById(1L)).thenReturn(Optional.of(grupo));
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.registrarComoEstudiante(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya está registrado como estudiante");
        }

        @Test
        @DisplayName("Lanza excepción si usuario no existe")
        void registrar_usuarioNoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.registrarComoEstudiante(99L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si grupo no existe")
        void registrar_grupoNoExiste() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.registrarComoEstudiante(1L, 99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("esProfesor / esEstudiante")
    class RolChecks {

        @Test
        @DisplayName("esProfesor devuelve true")
        void esProfesor_true() {
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(true);
            assertThat(usuarioService.esProfesor(1L)).isTrue();
        }

        @Test
        @DisplayName("esProfesor devuelve false")
        void esProfesor_false() {
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(false);
            assertThat(usuarioService.esProfesor(1L)).isFalse();
        }

        @Test
        @DisplayName("esEstudiante devuelve true")
        void esEstudiante_true() {
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(true);
            assertThat(usuarioService.esEstudiante(1L)).isTrue();
        }

        @Test
        @DisplayName("esEstudiante devuelve false")
        void esEstudiante_false() {
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(false);
            assertThat(usuarioService.esEstudiante(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("obtenerProfesorId / obtenerEstudianteId")
    class ObtenerIds {

        @Test
        @DisplayName("Obtiene profesorId")
        void profesorId_ok() {
            when(profesorRepository.findFirstByUsuarioId(1L)).thenReturn(Optional.of(profesor));

            Long result = usuarioService.obtenerProfesorId(1L);

            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no es profesor")
        void profesorId_noEsProfesor() {
            when(profesorRepository.findFirstByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obtenerProfesorId(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Obtiene estudianteId")
        void estudianteId_ok() {
            when(estudianteRepository.findFirstByUsuarioId(1L)).thenReturn(Optional.of(estudiante));

            Long result = usuarioService.obtenerEstudianteId(1L);

            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no es estudiante")
        void estudianteId_noEsEstudiante() {
            when(estudianteRepository.findFirstByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obtenerEstudianteId(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("eliminarRolProfesor")
    class EliminarRolProfesor {

        @Test
        @DisplayName("Elimina todos los roles de profesor del usuario")
        void eliminar_ok() {
            Profesor profesor2 = Profesor.builder().id(2L).usuario(usuario).build();
            when(profesorRepository.findByUsuarioId(1L)).thenReturn(List.of(profesor, profesor2));

            usuarioService.eliminarRolProfesor(1L);

            verify(profesorRepository).deleteAll(argThat(iterable -> {
                long count = 0;
                for (Profesor ignored : iterable) {
                    count++;
                }
                return count == 2;
            }));
        }

        @Test
        @DisplayName("Lanza excepción si el usuario no es profesor")
        void eliminar_noEsProfesor() {
            when(profesorRepository.findByUsuarioId(99L)).thenReturn(List.of());

            assertThatThrownBy(() -> usuarioService.eliminarRolProfesor(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("no es profesor");
        }
    }

    @Nested
    @DisplayName("eliminarRolEstudiante")
    class EliminarRolEstudiante {

        @Test
        @DisplayName("Elimina todos los roles de estudiante del usuario")
        void eliminar_ok() {
            Estudiante estudiante2 = Estudiante.builder().id(2L).usuario(usuario).grupo(grupo).build();
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(List.of(estudiante, estudiante2));

            usuarioService.eliminarRolEstudiante(1L);

            verify(estudianteRepository).deleteAll(argThat(iterable -> {
                long count = 0;
                for (Estudiante ignored : iterable) {
                    count++;
                }
                return count == 2;
            }));
        }

        @Test
        @DisplayName("Lanza excepción si el usuario no es estudiante")
        void eliminar_noEsEstudiante() {
            when(estudianteRepository.findByUsuarioId(99L)).thenReturn(List.of());

            assertThatThrownBy(() -> usuarioService.eliminarRolEstudiante(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("no es estudiante");
        }
    }

    @Nested
    @DisplayName("listarEstudiantesDeGrupo")
    class ListarEstudiantesDeGrupo {

        @Test
        @DisplayName("Lista usuarios estudiantes del grupo")
        void listar_ok() {
            Usuario user2 = Usuario.builder().id(2L).nombre("Ana").correoElectronico("ana@test.com").build();
            Estudiante est2 = Estudiante.builder().id(2L).usuario(user2).grupo(grupo).build();
            UsuarioDTO dto2 = UsuarioDTO.builder().id(2L).nombre("Ana").correoElectronico("ana@test.com").build();

            when(estudianteRepository.findByGrupoId(1L)).thenReturn(List.of(estudiante, est2));
            when(mapper.toDTO(usuario)).thenReturn(usuarioDTO);
            when(mapper.toDTO(user2)).thenReturn(dto2);

            List<UsuarioDTO> result = usuarioService.listarEstudiantesDeGrupo(1L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UsuarioDTO::getNombre).containsExactly("Juan", "Ana");
        }

        @Test
        @DisplayName("Devuelve vacío si no hay estudiantes en el grupo")
        void listar_vacio() {
            when(estudianteRepository.findByGrupoId(1L)).thenReturn(List.of());

            List<UsuarioDTO> result = usuarioService.listarEstudiantesDeGrupo(1L);

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("eliminarEstudianteDeGrupo")
    class EliminarEstudianteDeGrupo {

        @Test
        @DisplayName("Elimina estudiante cuando pertenece al grupo")
        void eliminar_ok() {
            when(estudianteRepository.findByUsuarioIdAndGrupoId(1L, 1L)).thenReturn(Optional.of(estudiante));

            usuarioService.eliminarEstudianteDeGrupo(1L, 1L);

            verify(estudianteRepository).delete(estudiante);
        }

        @Test
        @DisplayName("Lanza excepción cuando no pertenece al grupo")
        void eliminar_noPerteneceAlGrupo() {
            when(estudianteRepository.findByUsuarioIdAndGrupoId(1L, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.eliminarEstudianteDeGrupo(1L, 99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("no es estudiante del grupo");
        }
    }
}
