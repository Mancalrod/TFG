import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockPut } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPut: vi.fn(),
}));

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    put: mockPut,
  },
}));

import { notificacionService } from '../notificacionService';

describe('notificacionService', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPut.mockReset();
  });

  it('listar devuelve notificaciones desde endpoint base', async () => {
    const data = [{ id: 1, titulo: 'Nueva entrega' }];
    mockGet.mockResolvedValue({ data });

    const result = await notificacionService.listar();

    expect(mockGet).toHaveBeenCalledWith('/api/notificaciones');
    expect(result).toEqual(data);
  });

  it('marcarComoLeida usa endpoint con id', async () => {
    mockPut.mockResolvedValue({});

    await notificacionService.marcarComoLeida(7);

    expect(mockPut).toHaveBeenCalledWith('/api/notificaciones/7/leida');
  });

  it('contarNoLeidas devuelve count', async () => {
    mockGet.mockResolvedValue({ data: { count: 4 } });

    const count = await notificacionService.contarNoLeidas();

    expect(mockGet).toHaveBeenCalledWith('/api/notificaciones/no-leidas/count');
    expect(count).toBe(4);
  });

  it('obtiene y actualiza preferencias', async () => {
    const preferencias = {
      usuarioId: 3,
      canalEntregaCreada: 'APP',
      canalEntregaVencida: 'EMAIL',
      canalFeedbackRecibido: 'AMBOS',
      canalRecordatorio: 'APP',
      recordatorioDiasAntes: 2,
    };

    mockGet.mockResolvedValue({ data: preferencias });
    mockPut.mockResolvedValue({ data: preferencias });

    const actual = await notificacionService.obtenerPreferencias();
    const updated = await notificacionService.actualizarPreferencias(preferencias as never);

    expect(mockGet).toHaveBeenCalledWith('/api/notificaciones/preferencias');
    expect(mockPut).toHaveBeenCalledWith('/api/notificaciones/preferencias', preferencias);
    expect(actual).toEqual(preferencias);
    expect(updated).toEqual(preferencias);
  });
});
