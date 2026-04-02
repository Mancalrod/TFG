import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
  },
}));

import { cursoService } from '../cursoService';

describe('cursoService', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
  });

  it('GETs de cursos usan endpoints esperados', async () => {
    mockGet
      .mockResolvedValueOnce({ data: [{ id: 1, titulo: 'Todos', codigo: 'ALL', grupos: [] }] })
      .mockResolvedValueOnce({ data: { id: 2, titulo: 'Detalle', codigo: 'DET', grupos: [] } })
      .mockResolvedValueOnce({ data: { id: 3, titulo: 'PorCodigo', codigo: 'COD', grupos: [] } })
      .mockResolvedValueOnce({ data: [{ id: 4, titulo: 'Profesor', codigo: 'PRF', grupos: [] }] })
      .mockResolvedValueOnce({ data: [{ id: 5, titulo: 'Estudiante', codigo: 'EST', grupos: [] }] })
      .mockResolvedValueOnce({ data: [{ id: 6, titulo: 'Grupo A', numeroEstudiantes: 10 }] })
      .mockResolvedValueOnce({ data: [{ id: 7, titulo: 'Grupo Global', numeroEstudiantes: 15 }] });

    const todos = await cursoService.listarTodos();
    const detalle = await cursoService.obtener(2);
    const porCodigo = await cursoService.obtenerPorCodigo('COD');
    const porProfesor = await cursoService.listarPorProfesor(8);
    const porEstudiante = await cursoService.listarPorEstudiante(9);
    const grupos = await cursoService.listarGrupos(20);
    const todosLosGrupos = await cursoService.listarTodosGrupos();

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/cursos');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/cursos/2');
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/cursos/codigo/COD');
    expect(mockGet).toHaveBeenNthCalledWith(4, '/api/cursos/profesor/8');
    expect(mockGet).toHaveBeenNthCalledWith(5, '/api/cursos/estudiante/9');
    expect(mockGet).toHaveBeenNthCalledWith(6, '/api/cursos/20/grupos');
    expect(mockGet).toHaveBeenNthCalledWith(7, '/api/cursos/grupos');
    expect(todos).toHaveLength(1);
    expect(detalle.id).toBe(2);
    expect(porCodigo.codigo).toBe('COD');
    expect(porProfesor).toHaveLength(1);
    expect(porEstudiante).toHaveLength(1);
    expect(grupos).toHaveLength(1);
    expect(todosLosGrupos).toHaveLength(1);
  });

  it('POST de cursos y profesores usa rutas esperadas', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Curso Nuevo' } })
      .mockResolvedValueOnce({ data: { id: 11, titulo: 'Curso Usuario' } })
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Con Profesor' } })
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Con Profesor Usuario' } })
      .mockResolvedValueOnce({ data: { id: 99, titulo: 'Grupo A' } })
      .mockResolvedValueOnce({ data: { id: 120, titulo: 'Grupo con cursos' } });

    const creado = await cursoService.crear(7, {
      titulo: 'Curso Nuevo',
      descripcion: 'Desc',
      codigo: 'NUE-1',
    });

    const creadoPorUsuario = await cursoService.crearPorUsuario(21, {
      titulo: 'Curso Usuario',
      descripcion: 'Desc User',
      codigo: 'USR-1',
    });

    const conProfesor = await cursoService.agregarProfesor(10, 7);
    const conProfesorUsuario = await cursoService.agregarProfesorPorUsuario(10, 21);

    const grupo = await cursoService.crearGrupo(10, 'Grupo A');
    const grupoConCursos = await cursoService.crearGrupoConCursos({
      titulo: 'Grupo Unificado',
      cursoIds: [10, 11],
    });

    expect(mockPost).toHaveBeenNthCalledWith(1, '/api/cursos/profesor/7', {
      titulo: 'Curso Nuevo',
      descripcion: 'Desc',
      codigo: 'NUE-1',
    });
    expect(mockPost).toHaveBeenNthCalledWith(2, '/api/cursos/usuario/21', {
      titulo: 'Curso Usuario',
      descripcion: 'Desc User',
      codigo: 'USR-1',
    });
    expect(mockPost).toHaveBeenNthCalledWith(3, '/api/cursos/10/profesores/7');
    expect(mockPost).toHaveBeenNthCalledWith(4, '/api/cursos/10/usuarios/21/profesor');

    expect(mockPost).toHaveBeenNthCalledWith(5, '/api/cursos/10/grupos', null, {
      params: { titulo: 'Grupo A' },
    });
    expect(mockPost).toHaveBeenNthCalledWith(6, '/api/cursos/grupos', {
      titulo: 'Grupo Unificado',
      cursoIds: [10, 11],
    });

    expect(creado.id).toBe(10);
    expect(creadoPorUsuario.id).toBe(11);
    expect(conProfesor.id).toBe(10);
    expect(conProfesorUsuario.id).toBe(10);
    expect(grupo.id).toBe(99);
    expect(grupoConCursos.id).toBe(120);
  });

  it('PUT y DELETE usan endpoints correctos', async () => {
    mockPut
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Curso Actualizado' } })
      .mockResolvedValueOnce({ data: { id: 33, titulo: 'Grupo Renombrado' } })
      .mockResolvedValueOnce({ data: { id: 33, titulo: 'Grupo + Cursos' } });

    mockDelete
      .mockResolvedValueOnce({ data: undefined })
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Sin profesor' } })
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Sin profesor por usuario' } })
      .mockResolvedValueOnce({ data: undefined });

    const actualizado = await cursoService.actualizar(10, {
      titulo: 'Curso Actualizado',
      descripcion: 'Nueva desc',
      codigo: 'UPD-1',
    });

    const grupoActualizado = await cursoService.actualizarGrupo(33, 'Grupo Renombrado');
    const grupoConCursosActualizado = await cursoService.actualizarGrupoConCursos(33, {
      titulo: 'Grupo + Cursos',
      cursoIds: [10],
    });

    await cursoService.eliminar(10);
    const sinProfesor = await cursoService.quitarProfesor(10, 7);
    const sinProfesorUsuario = await cursoService.quitarProfesorPorUsuario(10, 21);
    await cursoService.eliminarGrupo(33);

    expect(mockPut).toHaveBeenNthCalledWith(1, '/api/cursos/10', {
      titulo: 'Curso Actualizado',
      descripcion: 'Nueva desc',
      codigo: 'UPD-1',
    });
    expect(mockPut).toHaveBeenNthCalledWith(2, '/api/cursos/grupos/33', null, {
      params: { titulo: 'Grupo Renombrado' },
    });
    expect(mockPut).toHaveBeenNthCalledWith(3, '/api/cursos/grupos/33/cursos', {
      titulo: 'Grupo + Cursos',
      cursoIds: [10],
    });

    expect(mockDelete).toHaveBeenNthCalledWith(1, '/api/cursos/10');
    expect(mockDelete).toHaveBeenNthCalledWith(2, '/api/cursos/10/profesores/7');
    expect(mockDelete).toHaveBeenNthCalledWith(3, '/api/cursos/10/usuarios/21/profesor');
    expect(mockDelete).toHaveBeenNthCalledWith(4, '/api/cursos/grupos/33');

    expect(actualizado.id).toBe(10);
    expect(grupoActualizado.id).toBe(33);
    expect(grupoConCursosActualizado.id).toBe(33);
    expect(sinProfesor.id).toBe(10);
    expect(sinProfesorUsuario.id).toBe(10);
  });
});

