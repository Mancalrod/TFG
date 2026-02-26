package com.tfg.gestionentregables.config;

import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.*;
import com.tfg.gestionentregables.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeder de datos iniciales para desarrollo.
 * Solo se ejecuta con el perfil "dev" o por defecto si no hay perfil activo.
 * Carga datos de ejemplo para todas las entidades del sistema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class DataSeeder implements CommandLineRunner {

    private static final String SEPARATOR = "========================================";
    private static final String PROF_PASSWORD = "prof123";
    private static final String ALUMNO_PASSWORD = "alumno123";

    private final UsuarioRepository usuarioRepository;
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;
    private final CursoRepository cursoRepository;
    private final GrupoRepository grupoRepository;
    private final ActividadRepository actividadRepository;
    private final EntregableRepository entregableRepository;
    private final EntregaRepository entregaRepository;
    private final MaterialRepository materialRepository;
    private final FeedbackRepository feedbackRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("La base de datos ya contiene datos. Se omite el seeding.");
            return;
        }

        log.info(SEPARATOR);
        log.info("Iniciando seeding de datos de desarrollo");
        log.info(SEPARATOR);

        // 1. Usuarios
        List<Usuario> usuarios = seedUsuarios();

        // 2. Cursos
        List<Curso> cursos = seedCursos();

        // 3. Profesores (relación usuario-curso)
        seedProfesores(usuarios, cursos);

        // 4. Grupos
        List<Grupo> grupos = seedGrupos(cursos);

        // 5. Estudiantes (relación usuario-grupo)
        List<Estudiante> estudiantes = seedEstudiantes(usuarios, grupos);

        // 6. Actividades
        List<Actividad> actividades = seedActividades(cursos, grupos);

        // 7. Materiales de actividades
        seedMaterialesActividad(actividades);

        // 8. Entregables
        List<Entregable> entregables = seedEntregables(actividades);

        // 9. Materiales de entregables
        seedMaterialesEntregable(entregables);

        // 10. Entregas
        List<Entrega> entregas = seedEntregas(entregables, estudiantes);

        // 11. Materiales de entregas
        seedMaterialesEntrega(entregas);

        // 12. Feedbacks
        seedFeedbacks(entregas, usuarios);

        log.info(SEPARATOR);
        log.info("Seeding completado exitosamente");
        log.info(SEPARATOR);
        log.info("Credenciales de prueba:");
        log.info("  Admin:      admin@ull.edu.es / admin123");
        log.info("  Profesor 1: juan.garcia@ull.edu.es / {}", PROF_PASSWORD);
        log.info("  Profesor 2: maria.lopez@ull.edu.es / {}", PROF_PASSWORD);
        log.info("  Profesor 3: carlos.martinez@ull.edu.es / {}", PROF_PASSWORD);
        log.info("  Alumno 1:   ana.fernandez@ull.edu.es / {}", ALUMNO_PASSWORD);
        log.info("  Alumno 2:   pedro.sanchez@ull.edu.es / {}", ALUMNO_PASSWORD);
        log.info("  Alumno 3:   laura.diaz@ull.edu.es / {}", ALUMNO_PASSWORD);
        log.info("  Alumno 4:   miguel.ruiz@ull.edu.es / {}", ALUMNO_PASSWORD);
        log.info("  Alumno 5:   sofia.moreno@ull.edu.es / {}", ALUMNO_PASSWORD);
        log.info("  Alumno 6:   daniel.jimenez@ull.edu.es / {}", ALUMNO_PASSWORD);
        log.info(SEPARATOR);
    }

    // =============================================
    // 1. USUARIOS
    // =============================================
    private List<Usuario> seedUsuarios() {
        log.info("Seeding usuarios...");

        Usuario admin = Usuario.builder()
                .nombre("Administrador del Sistema")
                .telefono("922000000")
                .correoElectronico("admin@ull.edu.es")
                .contrasena(passwordEncoder.encode("admin123"))
                .esAdmin(true)
                .build();

        Usuario prof1 = Usuario.builder()
                .nombre("Juan García Pérez")
                .telefono("922111111")
                .correoElectronico("juan.garcia@ull.edu.es")
                .contrasena(passwordEncoder.encode(PROF_PASSWORD))
                .esAdmin(false)
                .build();

        Usuario prof2 = Usuario.builder()
                .nombre("María López Hernández")
                .telefono("922222222")
                .correoElectronico("maria.lopez@ull.edu.es")
                .contrasena(passwordEncoder.encode("prof123"))
                .esAdmin(false)
                .build();

        Usuario prof3 = Usuario.builder()
                .nombre("Carlos Martínez Rodríguez")
                .telefono("922333333")
                .correoElectronico("carlos.martinez@ull.edu.es")
                .contrasena(passwordEncoder.encode("prof123"))
                .esAdmin(false)
                .build();

        Usuario est1 = Usuario.builder()
                .nombre("Ana Fernández Torres")
                .telefono("622111111")
                .correoElectronico("ana.fernandez@ull.edu.es")
                .contrasena(passwordEncoder.encode(ALUMNO_PASSWORD))
                .esAdmin(false)
                .build();

        Usuario est2 = Usuario.builder()
                .nombre("Pedro Sánchez Ramos")
                .telefono("622222222")
                .correoElectronico("pedro.sanchez@ull.edu.es")
                .contrasena(passwordEncoder.encode("alumno123"))
                .esAdmin(false)
                .build();

        Usuario est3 = Usuario.builder()
                .nombre("Laura Díaz Molina")
                .telefono("622333333")
                .correoElectronico("laura.diaz@ull.edu.es")
                .contrasena(passwordEncoder.encode("alumno123"))
                .esAdmin(false)
                .build();

        Usuario est4 = Usuario.builder()
                .nombre("Miguel Ruiz Navarro")
                .telefono("622444444")
                .correoElectronico("miguel.ruiz@ull.edu.es")
                .contrasena(passwordEncoder.encode("alumno123"))
                .esAdmin(false)
                .build();

        Usuario est5 = Usuario.builder()
                .nombre("Sofía Moreno Castillo")
                .telefono("622555555")
                .correoElectronico("sofia.moreno@ull.edu.es")
                .contrasena(passwordEncoder.encode("alumno123"))
                .esAdmin(false)
                .build();

        Usuario est6 = Usuario.builder()
                .nombre("Daniel Jiménez Ortega")
                .telefono("622666666")
                .correoElectronico("daniel.jimenez@ull.edu.es")
                .contrasena(passwordEncoder.encode("alumno123"))
                .esAdmin(false)
                .build();

        List<Usuario> usuarios = usuarioRepository.saveAll(
                List.of(admin, prof1, prof2, prof3, est1, est2, est3, est4, est5, est6));
        log.info("  -> {} usuarios creados", usuarios.size());
        return usuarios;
    }

    // =============================================
    // 2. CURSOS
    // =============================================
    private List<Curso> seedCursos() {
        log.info("Seeding cursos...");

        Curso curso1 = Curso.builder()
                .titulo("Ingeniería del Software")
                .descripcion("Asignatura de tercer curso del Grado en Ingeniería Informática. " +
                        "Se estudian metodologías ágiles, patrones de diseño y desarrollo de software.")
                .codigo("IS-301")
                .build();

        Curso curso2 = Curso.builder()
                .titulo("Bases de Datos Avanzadas")
                .descripcion("Asignatura de cuarto curso. Cubre bases de datos NoSQL, " +
                        "optimización de consultas y administración de SGBD.")
                .codigo("BDA-401")
                .build();

        Curso curso3 = Curso.builder()
                .titulo("Desarrollo Web Full Stack")
                .descripcion("Asignatura optativa de cuarto curso. Desarrollo de aplicaciones web " +
                        "con React, Spring Boot y despliegue en la nube.")
                .codigo("DWFS-402")
                .build();

        List<Curso> cursos = cursoRepository.saveAll(List.of(curso1, curso2, curso3));
        log.info("  -> {} cursos creados", cursos.size());
        return cursos;
    }

    // =============================================
    // 3. PROFESORES
    // =============================================
    private List<Profesor> seedProfesores(List<Usuario> usuarios, List<Curso> cursos) {
        log.info("Seeding profesores...");

        // prof1 (Juan) enseña IS y DWFS
        Profesor p1c1 = Profesor.builder()
                .usuario(usuarios.get(1)) // Juan
                .curso(cursos.get(0))     // IS
                .build();

        Profesor p1c3 = Profesor.builder()
                .usuario(usuarios.get(1)) // Juan
                .curso(cursos.get(2))     // DWFS
                .build();

        // prof2 (María) enseña BDA
        Profesor p2c2 = Profesor.builder()
                .usuario(usuarios.get(2)) // María
                .curso(cursos.get(1))     // BDA
                .build();

        // prof3 (Carlos) enseña IS y BDA
        Profesor p3c1 = Profesor.builder()
                .usuario(usuarios.get(3)) // Carlos
                .curso(cursos.get(0))     // IS
                .build();

        Profesor p3c2 = Profesor.builder()
                .usuario(usuarios.get(3)) // Carlos
                .curso(cursos.get(1))     // BDA
                .build();

        List<Profesor> profesores = profesorRepository.saveAll(
                List.of(p1c1, p1c3, p2c2, p3c1, p3c2));
        log.info("  -> {} asignaciones profesor-curso creadas", profesores.size());
        return profesores;
    }

    // =============================================
    // 4. GRUPOS
    // =============================================
    private List<Grupo> seedGrupos(List<Curso> cursos) {
        log.info("Seeding grupos...");

        // Curso IS: 2 grupos
        Grupo g1 = Grupo.builder()
                .titulo("Grupo A - Mañana")
                .curso(cursos.get(0))
                .build();

        Grupo g2 = Grupo.builder()
                .titulo("Grupo B - Tarde")
                .curso(cursos.get(0))
                .build();

        // Curso BDA: 2 grupos
        Grupo g3 = Grupo.builder()
                .titulo("Grupo Único")
                .curso(cursos.get(1))
                .build();

        Grupo g4 = Grupo.builder()
                .titulo("Grupo Laboratorio")
                .curso(cursos.get(1))
                .build();

        // Curso DWFS: 1 grupo
        Grupo g5 = Grupo.builder()
                .titulo("Grupo Prácticas")
                .curso(cursos.get(2))
                .build();

        List<Grupo> grupos = grupoRepository.saveAll(List.of(g1, g2, g3, g4, g5));
        log.info("  -> {} grupos creados", grupos.size());
        return grupos;
    }

    // =============================================
    // 5. ESTUDIANTES
    // =============================================
    private List<Estudiante> seedEstudiantes(List<Usuario> usuarios, List<Grupo> grupos) {
        log.info("Seeding estudiantes...");

        // Ana y Pedro en Grupo A de IS
        Estudiante e1 = Estudiante.builder()
                .usuario(usuarios.get(4)) // Ana
                .grupo(grupos.get(0))     // Grupo A - IS
                .build();

        Estudiante e2 = Estudiante.builder()
                .usuario(usuarios.get(5)) // Pedro
                .grupo(grupos.get(0))     // Grupo A - IS
                .build();

        // Laura y Miguel en Grupo B de IS
        Estudiante e3 = Estudiante.builder()
                .usuario(usuarios.get(6)) // Laura
                .grupo(grupos.get(1))     // Grupo B - IS
                .build();

        Estudiante e4 = Estudiante.builder()
                .usuario(usuarios.get(7)) // Miguel
                .grupo(grupos.get(1))     // Grupo B - IS
                .build();

        // Ana y Sofía en Grupo Único de BDA
        Estudiante e5 = Estudiante.builder()
                .usuario(usuarios.get(4)) // Ana
                .grupo(grupos.get(2))     // Grupo Único - BDA
                .build();

        Estudiante e6 = Estudiante.builder()
                .usuario(usuarios.get(8)) // Sofía
                .grupo(grupos.get(2))     // Grupo Único - BDA
                .build();

        // Daniel en Grupo Laboratorio de BDA
        Estudiante e7 = Estudiante.builder()
                .usuario(usuarios.get(9)) // Daniel
                .grupo(grupos.get(3))     // Grupo Lab - BDA
                .build();

        // Pedro, Laura y Daniel en Grupo Prácticas de DWFS
        Estudiante e8 = Estudiante.builder()
                .usuario(usuarios.get(5)) // Pedro
                .grupo(grupos.get(4))     // Grupo Prácticas - DWFS
                .build();

        Estudiante e9 = Estudiante.builder()
                .usuario(usuarios.get(6)) // Laura
                .grupo(grupos.get(4))     // Grupo Prácticas - DWFS
                .build();

        Estudiante e10 = Estudiante.builder()
                .usuario(usuarios.get(9)) // Daniel
                .grupo(grupos.get(4))     // Grupo Prácticas - DWFS
                .build();

        List<Estudiante> estudiantes = estudianteRepository.saveAll(
                List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
        log.info("  -> {} asignaciones estudiante-grupo creadas", estudiantes.size());
        return estudiantes;
    }

    // =============================================
    // 6. ACTIVIDADES
    // =============================================
    private List<Actividad> seedActividades(List<Curso> cursos, List<Grupo> grupos) {
        log.info("Seeding actividades...");

        LocalDateTime ahora = LocalDateTime.now();

        // === Actividades del curso IS ===
        Actividad act1 = Actividad.builder()
                .titulo("Práctica 1: Análisis de Requisitos")
                .descripcion("Realizar el análisis de requisitos de un sistema de información. " +
                        "Incluir diagrama de casos de uso y especificación de requisitos funcionales y no funcionales.")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(ahora.minusDays(30))
                .fechaInicio(ahora.minusDays(28))
                .fechaLimite(ahora.minusDays(14))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .curso(cursos.get(0))
                .grupos(new HashSet<>(Set.of(grupos.get(0), grupos.get(1))))
                .build();

        Actividad act2 = Actividad.builder()
                .titulo("Práctica 2: Diseño del Sistema")
                .descripcion("Elaborar el diseño arquitectónico del sistema. " +
                        "Incluir diagramas de clases, secuencia y componentes usando UML.")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(ahora.minusDays(20))
                .fechaInicio(ahora.minusDays(18))
                .fechaLimite(ahora.plusDays(7))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .curso(cursos.get(0))
                .grupos(new HashSet<>(Set.of(grupos.get(0), grupos.get(1))))
                .build();

        Actividad act3 = Actividad.builder()
                .titulo("Seminario: Metodologías Ágiles")
                .descripcion("Asistir al seminario sobre Scrum y Kanban. No requiere entrega.")
                .tipoActividad(TipoActividad.NO_EVALUABLE)
                .fechaCreacion(ahora.minusDays(15))
                .fechaInicio(ahora.minusDays(10))
                .fechaLimite(ahora.plusDays(5))
                .visibilidad(Visibilidad.VISIBLE)
                .curso(cursos.get(0))
                .grupos(new HashSet<>(Set.of(grupos.get(0))))
                .build();

        Actividad act4 = Actividad.builder()
                .titulo("Práctica 3: Implementación")
                .descripcion("Implementar el sistema diseñado en la práctica anterior. " +
                        "Usar Spring Boot para el backend y React para el frontend.")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(ahora.minusDays(5))
                .fechaInicio(ahora)
                .fechaLimite(ahora.plusDays(21))
                .visibilidad(Visibilidad.OCULTO)
                .notaMaxima(10.0)
                .curso(cursos.get(0))
                .grupos(new HashSet<>(Set.of(grupos.get(0), grupos.get(1))))
                .build();

        // === Actividades del curso BDA ===
        Actividad act5 = Actividad.builder()
                .titulo("Práctica 1: Modelado NoSQL")
                .descripcion("Diseñar un modelo de datos para MongoDB. " +
                        "Justificar las decisiones de modelado según los patrones de acceso.")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(ahora.minusDays(25))
                .fechaInicio(ahora.minusDays(23))
                .fechaLimite(ahora.minusDays(10))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .curso(cursos.get(1))
                .grupos(new HashSet<>(Set.of(grupos.get(2), grupos.get(3))))
                .build();

        Actividad act6 = Actividad.builder()
                .titulo("Práctica 2: Optimización de consultas SQL")
                .descripcion("Analizar y optimizar un conjunto de consultas SQL mediante índices " +
                        "y reestructuración de queries. Documentar el plan de ejecución antes y después.")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(ahora.minusDays(10))
                .fechaInicio(ahora.minusDays(8))
                .fechaLimite(ahora.plusDays(14))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .curso(cursos.get(1))
                .grupos(new HashSet<>(Set.of(grupos.get(2))))
                .build();

        // === Actividades del curso DWFS ===
        Actividad act7 = Actividad.builder()
                .titulo("Proyecto: Aplicación Web Completa")
                .descripcion("Desarrollar una aplicación web completa con autenticación, " +
                        "CRUD y despliegue. Incluir documentación API con Swagger.")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(ahora.minusDays(20))
                .fechaInicio(ahora.minusDays(18))
                .fechaLimite(ahora.plusDays(30))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .curso(cursos.get(2))
                .grupos(new HashSet<>(Set.of(grupos.get(4))))
                .build();

        Actividad act8 = Actividad.builder()
                .titulo("Taller: Docker y CI/CD")
                .descripcion("Taller práctico de contenedores Docker y pipelines de CI/CD con GitHub Actions.")
                .tipoActividad(TipoActividad.NO_EVALUABLE)
                .fechaCreacion(ahora.minusDays(3))
                .fechaInicio(ahora.plusDays(2))
                .fechaLimite(ahora.plusDays(16))
                .visibilidad(Visibilidad.VISIBLE)
                .curso(cursos.get(2))
                .grupos(new HashSet<>(Set.of(grupos.get(4))))
                .build();

        List<Actividad> actividades = actividadRepository.saveAll(
                List.of(act1, act2, act3, act4, act5, act6, act7, act8));
        log.info("  -> {} actividades creadas", actividades.size());
        return actividades;
    }

    // =============================================
    // 7. MATERIALES DE ACTIVIDADES
    // =============================================
    private void seedMaterialesActividad(List<Actividad> actividades) {
        log.info("Seeding materiales de actividades...");

        Material mat1 = Material.builder()
                .nombre("Guía de Análisis de Requisitos.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/materiales/actividades/guia-analisis-requisitos.pdf")
                .tamanoBytes(2_500_000L)
                .actividad(actividades.get(0))
                .build();

        Material mat2 = Material.builder()
                .nombre("Plantilla ERS.docx")
                .tipoMaterial(TipoMaterial.DOCX)
                .ruta("/materiales/actividades/plantilla-ers.docx")
                .tamanoBytes(150_000L)
                .actividad(actividades.get(0))
                .build();

        Material mat3 = Material.builder()
                .nombre("Tutorial UML - Diagramas de Clases.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/materiales/actividades/tutorial-uml.pdf")
                .tamanoBytes(3_200_000L)
                .actividad(actividades.get(1))
                .build();

        Material mat4 = Material.builder()
                .nombre("Slides Metodologías Ágiles.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/materiales/actividades/slides-agiles.pdf")
                .tamanoBytes(5_000_000L)
                .actividad(actividades.get(2))
                .build();

        Material mat5 = Material.builder()
                .nombre("Dataset consultas SQL.zip")
                .tipoMaterial(TipoMaterial.ZIP)
                .ruta("/materiales/actividades/dataset-sql.zip")
                .tamanoBytes(10_000_000L)
                .actividad(actividades.get(5))
                .build();

        Material mat6 = Material.builder()
                .nombre("Enunciado Proyecto Web.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/materiales/actividades/enunciado-proyecto-web.pdf")
                .tamanoBytes(1_800_000L)
                .actividad(actividades.get(6))
                .build();

        List<Material> materiales = materialRepository.saveAll(
                List.of(mat1, mat2, mat3, mat4, mat5, mat6));
        log.info("  -> {} materiales de actividades creados", materiales.size());
    }

    // =============================================
    // 8. ENTREGABLES
    // =============================================
    private List<Entregable> seedEntregables(List<Actividad> actividades) {
        log.info("Seeding entregables...");

        LocalDateTime ahora = LocalDateTime.now();

        // Entregables para Práctica 1: Análisis de Requisitos (ya vencida)
        Entregable ent1 = Entregable.builder()
                .titulo("Documento ERS")
                .descripcion("Entregar el documento de Especificación de Requisitos del Sistema en formato PDF.")
                .fechaInicio(ahora.minusDays(28))
                .fechaLimite(ahora.minusDays(14))
                .notaMaxima(7.0)
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(10_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(0))
                .build();

        Entregable ent2 = Entregable.builder()
                .titulo("Diagrama de Casos de Uso")
                .descripcion("Diagrama de casos de uso en formato imagen o PDF.")
                .fechaInicio(ahora.minusDays(28))
                .fechaLimite(ahora.minusDays(14))
                .notaMaxima(3.0)
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(5_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(false)
                .actividad(actividades.get(0))
                .build();

        // Entregables para Práctica 2: Diseño del Sistema (en plazo)
        Entregable ent3 = Entregable.builder()
                .titulo("Diagrama de Clases UML")
                .descripcion("Entregar el diagrama de clases del sistema en formato PDF o imagen.")
                .fechaInicio(ahora.minusDays(18))
                .fechaLimite(ahora.plusDays(5))
                .notaMaxima(5.0)
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(5_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(1))
                .build();

        Entregable ent4 = Entregable.builder()
                .titulo("Diagrama de Secuencia")
                .descripcion("Diagrama de secuencia para los 3 casos de uso principales.")
                .fechaInicio(ahora.minusDays(18))
                .fechaLimite(ahora.plusDays(7))
                .notaMaxima(5.0)
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(5_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(1))
                .build();

        // Entregable para Práctica 3: Implementación (oculto, futuro)
        Entregable ent5 = Entregable.builder()
                .titulo("Código fuente del proyecto")
                .descripcion("Subir el repositorio comprimido con el código fuente.")
                .fechaInicio(ahora)
                .fechaLimite(ahora.plusDays(21))
                .notaMaxima(10.0)
                .tipoArchivoEsperado(TipoMaterial.ZIP)
                .tamanoMaximoBytes(50_000_000L)
                .visibilidad(Visibilidad.OCULTO)
                .permiteReenvio(true)
                .actividad(actividades.get(3))
                .build();

        // Entregables para BDA - Práctica 1: Modelado NoSQL (ya vencida)
        Entregable ent6 = Entregable.builder()
                .titulo("Modelo de datos MongoDB")
                .descripcion("Documento con el diseño del modelo de datos para MongoDB y justificación.")
                .fechaInicio(ahora.minusDays(23))
                .fechaLimite(ahora.minusDays(10))
                .notaMaxima(10.0)
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(10_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(4))
                .build();

        // Entregables para BDA - Práctica 2: Optimización SQL (en plazo)
        Entregable ent7 = Entregable.builder()
                .titulo("Informe de optimización")
                .descripcion("Informe con el análisis de las consultas originales y optimizadas.")
                .fechaInicio(ahora.minusDays(8))
                .fechaLimite(ahora.plusDays(14))
                .notaMaxima(7.0)
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .tamanoMaximoBytes(10_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(5))
                .build();

        Entregable ent8 = Entregable.builder()
                .titulo("Scripts SQL optimizados")
                .descripcion("Archivo con los scripts SQL optimizados.")
                .fechaInicio(ahora.minusDays(8))
                .fechaLimite(ahora.plusDays(14))
                .notaMaxima(3.0)
                .tipoArchivoEsperado(TipoMaterial.TXT)
                .tamanoMaximoBytes(1_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(5))
                .build();

        // Entregables para DWFS - Proyecto Web (en plazo)
        Entregable ent9 = Entregable.builder()
                .titulo("Hito 1: Backend API REST")
                .descripcion("Primera entrega del proyecto: API REST funcional con documentación Swagger.")
                .fechaInicio(ahora.minusDays(18))
                .fechaLimite(ahora.plusDays(10))
                .notaMaxima(4.0)
                .tipoArchivoEsperado(TipoMaterial.ZIP)
                .tamanoMaximoBytes(50_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(6))
                .build();

        Entregable ent10 = Entregable.builder()
                .titulo("Hito 2: Frontend + Integración")
                .descripcion("Segunda entrega: aplicación completa con frontend React integrado.")
                .fechaInicio(ahora.plusDays(10))
                .fechaLimite(ahora.plusDays(30))
                .notaMaxima(6.0)
                .tipoArchivoEsperado(TipoMaterial.ZIP)
                .tamanoMaximoBytes(50_000_000L)
                .visibilidad(Visibilidad.VISIBLE)
                .permiteReenvio(true)
                .actividad(actividades.get(6))
                .build();

        List<Entregable> entregables = entregableRepository.saveAll(
                List.of(ent1, ent2, ent3, ent4, ent5, ent6, ent7, ent8, ent9, ent10));
        log.info("  -> {} entregables creados", entregables.size());
        return entregables;
    }

    // =============================================
    // 9. MATERIALES DE ENTREGABLES
    // =============================================
    private void seedMaterialesEntregable(List<Entregable> entregables) {
        log.info("Seeding materiales de entregables...");

        Material mat1 = Material.builder()
                .nombre("Ejemplo ERS - Proyecto de referencia.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/materiales/entregables/ejemplo-ers.pdf")
                .tamanoBytes(1_200_000L)
                .entregable(entregables.get(0))
                .build();

        Material mat2 = Material.builder()
                .nombre("Rúbrica de evaluación - Diagrama Clases.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/materiales/entregables/rubrica-diagramas.pdf")
                .tamanoBytes(300_000L)
                .entregable(entregables.get(2))
                .build();

        Material mat3 = Material.builder()
                .nombre("Guía Swagger/OpenAPI.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/materiales/entregables/guia-swagger.pdf")
                .tamanoBytes(800_000L)
                .entregable(entregables.get(8))
                .build();

        List<Material> materiales = materialRepository.saveAll(List.of(mat1, mat2, mat3));
        log.info("  -> {} materiales de entregables creados", materiales.size());
    }

    // =============================================
    // 10. ENTREGAS
    // =============================================
    private List<Entrega> seedEntregas(List<Entregable> entregables, List<Estudiante> estudiantes) {
        log.info("Seeding entregas...");

        LocalDateTime ahora = LocalDateTime.now();

        // --- Entregas para "Documento ERS" (ent1, ya vencido) ---
        // Ana (e1) entregó a tiempo, 2 versiones
        Entrega entrega1v1 = Entrega.builder()
                .nombre("ERS - Versión borrador")
                .version(1)
                .fechaEntrega(ahora.minusDays(20))
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(false)
                .entregable(entregables.get(0))
                .estudiante(estudiantes.get(0)) // Ana - Grupo A IS
                .build();

        Entrega entrega1v2 = Entrega.builder()
                .nombre("ERS - Versión final")
                .version(2)
                .fechaEntrega(ahora.minusDays(15))
                .estado(EstadoEntrega.CALIFICADO)
                .calificacion(6.5)
                .fechaCalificacion(ahora.minusDays(12))
                .esVersionActiva(true)
                .entregable(entregables.get(0))
                .estudiante(estudiantes.get(0)) // Ana
                .build();

        // Pedro (e2) entregó a tiempo
        Entrega entrega2 = Entrega.builder()
                .nombre("Documento ERS - Pedro Sánchez")
                .version(1)
                .fechaEntrega(ahora.minusDays(16))
                .estado(EstadoEntrega.CALIFICADO)
                .calificacion(5.0)
                .fechaCalificacion(ahora.minusDays(11))
                .esVersionActiva(true)
                .entregable(entregables.get(0))
                .estudiante(estudiantes.get(1)) // Pedro - Grupo A IS
                .build();

        // Laura (e3) entregó tarde
        Entrega entrega3 = Entrega.builder()
                .nombre("ERS - Laura Díaz")
                .version(1)
                .fechaEntrega(ahora.minusDays(12))
                .estado(EstadoEntrega.CALIFICADO)
                .calificacion(4.5)
                .fechaCalificacion(ahora.minusDays(8))
                .esVersionActiva(true)
                .entregable(entregables.get(0))
                .estudiante(estudiantes.get(2)) // Laura - Grupo B IS
                .build();

        // Miguel (e4) entregó a tiempo
        Entrega entrega4 = Entrega.builder()
                .nombre("Requisitos del Sistema - Miguel")
                .version(1)
                .fechaEntrega(ahora.minusDays(17))
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregables.get(0))
                .estudiante(estudiantes.get(3)) // Miguel - Grupo B IS
                .build();

        // --- Entregas para "Diagrama de Casos de Uso" (ent2, ya vencido) ---
        // Ana entregó
        Entrega entrega5 = Entrega.builder()
                .nombre("Casos de uso - Ana Fernández")
                .version(1)
                .fechaEntrega(ahora.minusDays(15))
                .estado(EstadoEntrega.CALIFICADO)
                .calificacion(2.8)
                .fechaCalificacion(ahora.minusDays(12))
                .esVersionActiva(true)
                .entregable(entregables.get(1))
                .estudiante(estudiantes.get(0)) // Ana
                .build();

        // --- Entregas para "Diagrama de Clases UML" (ent3, en plazo) ---
        // Ana entregó
        Entrega entrega6 = Entrega.builder()
                .nombre("Diagrama de Clases - Ana")
                .version(1)
                .fechaEntrega(ahora.minusDays(2))
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregables.get(2))
                .estudiante(estudiantes.get(0)) // Ana
                .build();

        // Pedro entregó
        Entrega entrega7 = Entrega.builder()
                .nombre("UML Clases - Pedro")
                .version(1)
                .fechaEntrega(ahora.minusDays(1))
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregables.get(2))
                .estudiante(estudiantes.get(1)) // Pedro
                .build();

        // --- Entregas para "Modelo de datos MongoDB" (ent6, ya vencido) ---
        // Ana (e5 en BDA) entregó
        Entrega entrega8 = Entrega.builder()
                .nombre("Modelo MongoDB - Ana Fernández")
                .version(1)
                .fechaEntrega(ahora.minusDays(12))
                .estado(EstadoEntrega.CALIFICADO)
                .calificacion(8.5)
                .fechaCalificacion(ahora.minusDays(7))
                .esVersionActiva(true)
                .entregable(entregables.get(5))
                .estudiante(estudiantes.get(4)) // Ana en BDA
                .build();

        // Sofía (e6 en BDA) entregó
        Entrega entrega9 = Entrega.builder()
                .nombre("MongoDB Data Model - Sofía")
                .version(1)
                .fechaEntrega(ahora.minusDays(11))
                .estado(EstadoEntrega.CALIFICADO)
                .calificacion(7.0)
                .fechaCalificacion(ahora.minusDays(6))
                .esVersionActiva(true)
                .entregable(entregables.get(5))
                .estudiante(estudiantes.get(5)) // Sofía en BDA
                .build();

        // Daniel (e7 en BDA) entregó tarde
        Entrega entrega10 = Entrega.builder()
                .nombre("NoSQL Model - Daniel")
                .version(1)
                .fechaEntrega(ahora.minusDays(8))
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregables.get(5))
                .estudiante(estudiantes.get(6)) // Daniel en BDA
                .build();

        // --- Entregas para "Hito 1: Backend API REST" (ent9, en plazo) ---
        // Pedro (e8 en DWFS) entregó
        Entrega entrega11 = Entrega.builder()
                .nombre("API REST - Pedro Sánchez")
                .version(1)
                .fechaEntrega(ahora.minusDays(3))
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregables.get(8))
                .estudiante(estudiantes.get(7)) // Pedro en DWFS
                .build();

        // Laura (e9 en DWFS) entregó
        Entrega entrega12 = Entrega.builder()
                .nombre("Backend Spring Boot - Laura")
                .version(1)
                .fechaEntrega(ahora.minusDays(2))
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregables.get(8))
                .estudiante(estudiantes.get(8)) // Laura en DWFS
                .build();

        List<Entrega> entregas = entregaRepository.saveAll(List.of(
                entrega1v1, entrega1v2, entrega2, entrega3, entrega4,
                entrega5, entrega6, entrega7, entrega8, entrega9,
                entrega10, entrega11, entrega12));
        log.info("  -> {} entregas creadas", entregas.size());
        return entregas;
    }

    // =============================================
    // 11. MATERIALES DE ENTREGAS
    // =============================================
    private void seedMaterialesEntrega(List<Entrega> entregas) {
        log.info("Seeding materiales de entregas...");

        // Archivos de la entrega de Ana (ERS v2)
        Material m1 = Material.builder()
                .nombre("ERS_AnaFernandez_v2.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entregas/1/ers-ana-v2.pdf")
                .tamanoBytes(2_100_000L)
                .entrega(entregas.get(1)) // entrega1v2
                .build();

        // Archivos de la entrega de Pedro (ERS)
        Material m2 = Material.builder()
                .nombre("ERS_PedroSanchez.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entregas/2/ers-pedro.pdf")
                .tamanoBytes(1_800_000L)
                .entrega(entregas.get(2)) // entrega2
                .build();

        // Archivos de la entrega de Laura (ERS)
        Material m3 = Material.builder()
                .nombre("ERS_LauraDiaz.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entregas/3/ers-laura.pdf")
                .tamanoBytes(1_950_000L)
                .entrega(entregas.get(3)) // entrega3
                .build();

        // Archivos de entrega diagrama casos de uso de Ana
        Material m4 = Material.builder()
                .nombre("CasosDeUso_Ana.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entregas/5/casos-uso-ana.pdf")
                .tamanoBytes(750_000L)
                .entrega(entregas.get(5)) // entrega5
                .build();

        // Diagrama de clases de Ana
        Material m5 = Material.builder()
                .nombre("DiagramaClases_Ana.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entregas/6/diagrama-clases-ana.pdf")
                .tamanoBytes(900_000L)
                .entrega(entregas.get(6)) // entrega6
                .build();

        // Modelo MongoDB de Ana
        Material m6 = Material.builder()
                .nombre("ModeloMongoDB_Ana.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entregas/8/modelo-mongo-ana.pdf")
                .tamanoBytes(1_500_000L)
                .entrega(entregas.get(8)) // entrega8
                .build();

        // Modelo MongoDB de Sofía
        Material m7 = Material.builder()
                .nombre("MongoDB_Sofia.pdf")
                .tipoMaterial(TipoMaterial.PDF)
                .ruta("/uploads/entregas/9/mongo-sofia.pdf")
                .tamanoBytes(1_300_000L)
                .entrega(entregas.get(9)) // entrega9
                .build();

        // API REST de Pedro (ZIP)
        Material m8 = Material.builder()
                .nombre("api-rest-pedro.zip")
                .tipoMaterial(TipoMaterial.ZIP)
                .ruta("/uploads/entregas/11/api-pedro.zip")
                .tamanoBytes(15_000_000L)
                .entrega(entregas.get(11)) // entrega11
                .build();

        // Backend Spring Boot de Laura (ZIP)
        Material m9 = Material.builder()
                .nombre("backend-laura.zip")
                .tipoMaterial(TipoMaterial.ZIP)
                .ruta("/uploads/entregas/12/backend-laura.zip")
                .tamanoBytes(18_000_000L)
                .entrega(entregas.get(12)) // entrega12
                .build();

        List<Material> materiales = materialRepository.saveAll(
                List.of(m1, m2, m3, m4, m5, m6, m7, m8, m9));
        log.info("  -> {} materiales de entregas creados", materiales.size());
    }

    // =============================================
    // 12. FEEDBACKS
    // =============================================
    private void seedFeedbacks(List<Entrega> entregas, List<Usuario> usuarios) {
        log.info("Seeding feedbacks...");

        LocalDateTime ahora = LocalDateTime.now();
        Usuario profJuan = usuarios.get(1);
        Usuario profMaria = usuarios.get(2);
        Usuario profCarlos = usuarios.get(3);

        // Feedback de Juan para la entrega de Ana (ERS v2)
        Feedback fb1 = Feedback.builder()
                .comentario("Buen trabajo en general. La sección de requisitos no funcionales necesita " +
                        "más detalle, especialmente en rendimiento y seguridad. El diagrama de contexto " +
                        "está muy bien elaborado.")
                .fechaCreacion(ahora.minusDays(12))
                .fechaModificacion(ahora.minusDays(12))
                .entrega(entregas.get(1)) // Ana ERS v2
                .profesor(profJuan)
                .build();

        // Feedback de Carlos para la entrega de Ana (ERS v2)
        Feedback fb2 = Feedback.builder()
                .comentario("Añadir las restricciones de negocio en la sección 3.2. " +
                        "Revisar la numeración de los requisitos funcionales.")
                .fechaCreacion(ahora.minusDays(11))
                .fechaModificacion(ahora.minusDays(11))
                .entrega(entregas.get(1)) // Ana ERS v2
                .profesor(profCarlos)
                .build();

        // Feedback de Juan para la entrega de Pedro (ERS)
        Feedback fb3 = Feedback.builder()
                .comentario("El documento cumple con los requisitos mínimos pero le falta profundidad. " +
                        "Faltan diagramas de apoyo y la trazabilidad de requisitos. Recomiendo revisar " +
                        "los ejemplos de clase.")
                .fechaCreacion(ahora.minusDays(11))
                .fechaModificacion(ahora.minusDays(11))
                .entrega(entregas.get(2)) // Pedro ERS
                .profesor(profJuan)
                .build();

        // Feedback de Juan para la entrega de Laura (ERS, entregada tarde)
        Feedback fb4 = Feedback.builder()
                .comentario("La entrega fue realizada fuera de plazo, lo cual penaliza la nota. " +
                        "El contenido es correcto pero incompleto. Faltan los casos de uso secundarios.")
                .fechaCreacion(ahora.minusDays(8))
                .fechaModificacion(ahora.minusDays(8))
                .entrega(entregas.get(3)) // Laura ERS
                .profesor(profJuan)
                .build();

        // Feedback de Juan para casos de uso de Ana
        Feedback fb5 = Feedback.builder()
                .comentario("Diagrama bien estructurado. Las relaciones include y extend están " +
                        "correctamente aplicadas. Falta el actor 'Sistema externo'.")
                .fechaCreacion(ahora.minusDays(12))
                .fechaModificacion(ahora.minusDays(12))
                .entrega(entregas.get(5)) // Ana Casos de Uso
                .profesor(profJuan)
                .build();

        // Feedback de María para modelo MongoDB de Ana
        Feedback fb6 = Feedback.builder()
                .comentario("Excelente trabajo. El modelo de datos está bien justificado y " +
                        "los patrones de acceso están claramente definidos. La desnormalización " +
                        "propuesta es adecuada para el caso de uso.")
                .fechaCreacion(ahora.minusDays(7))
                .fechaModificacion(ahora.minusDays(7))
                .entrega(entregas.get(8)) // Ana MongoDB
                .profesor(profMaria)
                .build();

        // Feedback de María para modelo MongoDB de Sofía
        Feedback fb7 = Feedback.builder()
                .comentario("Buen modelo. Hay algunas redundancias que podrían optimizarse. " +
                        "Revisar el esquema de la colección 'pedidos' y considerar usar " +
                        "referencias en lugar de documentos embebidos para los productos.")
                .fechaCreacion(ahora.minusDays(6))
                .fechaModificacion(ahora.minusDays(6))
                .entrega(entregas.get(9)) // Sofía MongoDB
                .profesor(profMaria)
                .build();

        // Feedback de Carlos para modelo MongoDB de Sofía (segundo feedback)
        Feedback fb8 = Feedback.builder()
                .comentario("Complementando el feedback de la profesora López: los índices " +
                        "propuestos son correctos para las consultas planteadas. Añadir un " +
                        "índice compuesto para la búsqueda por fecha y categoría.")
                .fechaCreacion(ahora.minusDays(5))
                .fechaModificacion(ahora.minusDays(5))
                .entrega(entregas.get(9)) // Sofía MongoDB
                .profesor(profCarlos)
                .build();

        List<Feedback> feedbacks = feedbackRepository.saveAll(
                List.of(fb1, fb2, fb3, fb4, fb5, fb6, fb7, fb8));
        log.info("  -> {} feedbacks creados", feedbacks.size());
    }
}
