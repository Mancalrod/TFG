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

import { entregableService } from '../entregableService';

describe('entregableService', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockPatch.mockReset();
    mockDelete.mockReset();
  });

  it('obtener y crear usan endpoints esperados', async () => {
    const payload = { titulo: 'Practica 1' } as never;
    mockGet.mockResolvedValue({ data: { id: 1 } });
    mockPost.mockResolvedValue({ data: { id: 2 } });

    await entregableService.obtener(1);
    await entregableService.crear(3, payload);

    expect(mockGet).toHaveBeenCalledWith('/api/entregables/1');
    expect(mockPost).toHaveBeenCalledWith('/api/entregables/actividad/3', payload);
  });

  it('listados por actividad usan rutas correctas', async () => {
    mockGet.mockResolvedValue({ data: [] });

    await entregableService.listarPorActividad(4);
    await entregableService.listarVisibles(4);
    await entregableService.listarEnPlazo(4);
    await entregableService.listarPendientes(4, 10);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/entregables/actividad/4');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/entregables/actividad/4/visibles');
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/entregables/actividad/4/en-plazo');
    expect(mockGet).toHaveBeenNthCalledWith(4, '/api/entregables/actividad/4/pendientes/10');
  });

  it('actualiza, cambia visibilidad y elimina', async () => {
    const payload = { titulo: 'Actualizado' } as never;
    mockPut.mockResolvedValue({ data: { id: 7 } });
    mockPatch.mockResolvedValue({ data: { id: 7, visibilidad: 'PUBLICA' } });
    mockDelete.mockResolvedValue({});

    await entregableService.actualizar(7, payload);
    await entregableService.cambiarVisibilidad(7, 'PUBLICA' as never);
    await entregableService.eliminar(7);

    expect(mockPut).toHaveBeenCalledWith('/api/entregables/7', payload);
    expect(mockPatch).toHaveBeenCalledWith('/api/entregables/7/visibilidad', null, {
      params: { visibilidad: 'PUBLICA' },
    });
    expect(mockDelete).toHaveBeenCalledWith('/api/entregables/7');
  });

  it('listarProximos usa dias por defecto y custom', async () => {
    mockGet.mockResolvedValue({ data: [] });

    await entregableService.listarProximos(5);
    await entregableService.listarProximos(5, 20);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/entregables/actividad/5/proximos', {
      params: { dias: 7 },
    });
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/entregables/actividad/5/proximos', {
      params: { dias: 20 },
    });
  });
});
