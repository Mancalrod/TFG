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
      .mockResolvedValueOnce({ data: [{ id: 6, titulo: 'Grupo A', numeroEstudiantes: 10 }] });

    const todos = await cursoService.listarTodos();
    const detalle = await cursoService.obtener(2);
    const porCodigo = await cursoService.obtenerPorCodigo('COD');
    const porProfesor = await cursoService.listarPorProfesor(8);
    const porEstudiante = await cursoService.listarPorEstudiante(9);
    const grupos = await cursoService.listarGrupos(20);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/cursos');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/cursos/2');
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/cursos/codigo/COD');
    expect(mockGet).toHaveBeenNthCalledWith(4, '/api/cursos/profesor/8');
    expect(mockGet).toHaveBeenNthCalledWith(5, '/api/cursos/estudiante/9');
    expect(mockGet).toHaveBeenNthCalledWith(6, '/api/cursos/20/grupos');
    expect(todos).toHaveLength(1);
    expect(detalle.id).toBe(2);
    expect(porCodigo.codigo).toBe('COD');
    expect(porProfesor).toHaveLength(1);
    expect(porEstudiante).toHaveLength(1);
    expect(grupos).toHaveLength(1);
  });

  it('POST de cursos y profesores usa rutas esperadas', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Curso Nuevo' } })
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Con Profesor' } })
      .mockResolvedValueOnce({ data: { id: 99, titulo: 'Grupo A' } });

    const creado = await cursoService.crear(7, {
      titulo: 'Curso Nuevo',
      descripcion: 'Desc',
      codigo: 'NUE-1',
    });

    const conProfesor = await cursoService.agregarProfesor(10, 7);

    const grupo = await cursoService.crearGrupo(10, 'Grupo A');

    expect(mockPost).toHaveBeenNthCalledWith(1, '/api/cursos/profesor/7', {
      titulo: 'Curso Nuevo',
      descripcion: 'Desc',
      codigo: 'NUE-1',
    });
    expect(mockPost).toHaveBeenNthCalledWith(2, '/api/cursos/10/profesores/7');

    expect(mockPost).toHaveBeenCalledWith('/api/cursos/10/grupos', null, {
      params: { titulo: 'Grupo A' },
    });

    expect(creado.id).toBe(10);
    expect(conProfesor.id).toBe(10);
    expect(grupo.id).toBe(99);
  });

  it('PUT y DELETE usan endpoints correctos', async () => {
    mockPut
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Curso Actualizado' } })
      .mockResolvedValueOnce({ data: { id: 33, titulo: 'Grupo Renombrado' } });

    mockDelete
      .mockResolvedValueOnce({ data: undefined })
      .mockResolvedValueOnce({ data: { id: 10, titulo: 'Sin profesor' } })
      .mockResolvedValueOnce({ data: undefined });

    const actualizado = await cursoService.actualizar(10, {
      titulo: 'Curso Actualizado',
      descripcion: 'Nueva desc',
      codigo: 'UPD-1',
    });

    const grupoActualizado = await cursoService.actualizarGrupo(33, 'Grupo Renombrado');

    await cursoService.eliminar(10);
    const sinProfesor = await cursoService.quitarProfesor(10, 7);
    await cursoService.eliminarGrupo(33);

    expect(mockPut).toHaveBeenNthCalledWith(1, '/api/cursos/10', {
      titulo: 'Curso Actualizado',
      descripcion: 'Nueva desc',
      codigo: 'UPD-1',
    });
    expect(mockPut).toHaveBeenNthCalledWith(2, '/api/cursos/grupos/33', null, {
      params: { titulo: 'Grupo Renombrado' },
    });

    expect(mockDelete).toHaveBeenNthCalledWith(1, '/api/cursos/10');
    expect(mockDelete).toHaveBeenNthCalledWith(2, '/api/cursos/10/profesores/7');
    expect(mockDelete).toHaveBeenNthCalledWith(3, '/api/cursos/grupos/33');

    expect(actualizado.id).toBe(10);
    expect(grupoActualizado.id).toBe(33);
    expect(sinProfesor.id).toBe(10);
  });
});

