import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockPost, mockPut, mockPatch, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockPatch: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    patch: mockPatch,
    delete: mockDelete,
  },
}));

import { actividadService } from '../actividadService';

describe('actividadService', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockPatch.mockReset();
    mockDelete.mockReset();
  });

  it('obtener usa endpoint base por id', async () => {
    mockGet.mockResolvedValue({ data: { id: 7 } });
    const data = await actividadService.obtener(7);
    expect(mockGet).toHaveBeenCalledWith('/api/actividades/7');
    expect(data).toEqual({ id: 7 });
  });

  it('obtenerConEntregables usa endpoint detalle', async () => {
    mockGet.mockResolvedValue({ data: { id: 7, detalle: true } });
    await actividadService.obtenerConEntregables(7);
    expect(mockGet).toHaveBeenCalledWith('/api/actividades/7/detalle');
  });

  it('crear envia payload y cursoId', async () => {
    const payload = { titulo: 'Actividad 1' } as never;
    mockPost.mockResolvedValue({ data: { id: 10 } });
    await actividadService.crear(5, payload);
    expect(mockPost).toHaveBeenCalledWith('/api/actividades/curso/5', payload);
  });

  it('listar por curso y grupo usa rutas correctas', async () => {
    mockGet.mockResolvedValue({ data: [] });
    await actividadService.listarPorCurso(2);
    await actividadService.listarPorGrupo(3);
    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/actividades/curso/2');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/actividades/grupo/3');
  });

  it('actualizar y cambiarVisibilidad usan endpoints y params', async () => {
    const payload = { titulo: 'Nueva' } as never;
    mockPut.mockResolvedValue({ data: { id: 4 } });
    mockPatch.mockResolvedValue({ data: { id: 4, visibilidad: 'PUBLICA' } });

    await actividadService.actualizar(4, payload);
    await actividadService.cambiarVisibilidad(4, 'PUBLICA' as never);

    expect(mockPut).toHaveBeenCalledWith('/api/actividades/4', payload);
    expect(mockPatch).toHaveBeenCalledWith('/api/actividades/4/visibilidad', null, {
      params: { visibilidad: 'PUBLICA' },
    });
  });

  it('eliminar y listados de plazo usan rutas esperadas', async () => {
    mockDelete.mockResolvedValue({});
    mockGet.mockResolvedValue({ data: [] });

    await actividadService.eliminar(9);
    await actividadService.listarEnPlazo(6);
    await actividadService.listarProximas(6, 12);

    expect(mockDelete).toHaveBeenCalledWith('/api/actividades/9');
    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/actividades/curso/6/en-plazo');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/actividades/curso/6/proximas', {
      params: { dias: 12 },
    });
  });
});