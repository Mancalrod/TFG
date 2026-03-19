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

import { feedbackService } from '../feedbackService';

describe('feedbackService', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
  });

  it('obtiene feedback por id', async () => {
    mockGet.mockResolvedValue({ data: { id: 10 } });

    const data = await feedbackService.obtener(10);

    expect(mockGet).toHaveBeenCalledWith('/api/feedback/10');
    expect(data).toEqual({ id: 10 });
  });

  it('crea feedback por entrega y profesor', async () => {
    const payload = { comentario: 'Buen trabajo' } as never;
    mockPost.mockResolvedValue({ data: { id: 11 } });

    await feedbackService.crear(7, 3, payload);

    expect(mockPost).toHaveBeenCalledWith('/api/feedback/entrega/7/profesor/3', payload);
  });

  it('lista por entrega y por profesor', async () => {
    mockGet.mockResolvedValue({ data: [] });

    await feedbackService.listarPorEntrega(4);
    await feedbackService.listarPorProfesor(9);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/feedback/entrega/4');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/feedback/profesor/9');
  });

  it('actualiza y elimina por id y profesor', async () => {
    const payload = { comentario: 'Actualizado' } as never;
    mockPut.mockResolvedValue({ data: { id: 12 } });
    mockDelete.mockResolvedValue({});

    await feedbackService.actualizar(12, 5, payload);
    await feedbackService.eliminar(12, 5);

    expect(mockPut).toHaveBeenCalledWith('/api/feedback/12/profesor/5', payload);
    expect(mockDelete).toHaveBeenCalledWith('/api/feedback/12/profesor/5');
  });

  it('contarRecientes usa params por defecto y personalizados', async () => {
    mockGet.mockResolvedValue({ data: 3 });

    await feedbackService.contarRecientes(8);
    await feedbackService.contarRecientes(8, 15);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/feedback/estudiante/8/recientes', {
      params: { dias: 7 },
    });
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/feedback/estudiante/8/recientes', {
      params: { dias: 15 },
    });
  });
});
