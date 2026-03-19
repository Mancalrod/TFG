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

  it('listarPorProfesor usa endpoint esperado', async () => {
    mockGet.mockResolvedValue({ data: [{ id: 10, titulo: 'Programacion I', codigo: 'PRG1', grupos: [] }] });

    const data = await cursoService.listarPorProfesor(1);

    expect(mockGet).toHaveBeenCalledWith('/api/cursos/profesor/1');
    expect(data).toHaveLength(1);
  });

  it('crearGrupo envia titulo como query param', async () => {
    mockPost.mockResolvedValue({ data: { id: 99, titulo: 'Grupo A' } });

    const grupo = await cursoService.crearGrupo(10, 'Grupo A');

    expect(mockPost).toHaveBeenCalledWith('/api/cursos/10/grupos', null, {
      params: { titulo: 'Grupo A' },
    });
    expect(grupo.id).toBe(99);
  });

  it('eliminarGrupo usa endpoint correcto', async () => {
    mockDelete.mockResolvedValue({ data: undefined });

    await cursoService.eliminarGrupo(33);

    expect(mockDelete).toHaveBeenCalledWith('/api/cursos/grupos/33');
  });
});

