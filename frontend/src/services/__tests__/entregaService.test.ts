import { describe, it, expect, vi, beforeEach } from 'vitest';

const { mockPost, mockGet, mockDelete } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock('../api', () => ({
  default: {
    post: mockPost,
    get: mockGet,
    delete: mockDelete,
  },
}));

import { entregaService } from '../entregaService';

describe('entregaService', () => {
  beforeEach(() => {
    mockPost.mockReset();
    mockGet.mockReset();
    mockDelete.mockReset();
  });

  it('envia nota y profesorId al calificar', async () => {
    const fakeResponse = { data: { id: 10, calificacion: 8.5 } };
    mockPost.mockResolvedValue(fakeResponse);

    await entregaService.calificar(10, 7, { nota: 8.5, comentario: 'Buen trabajo' });

    expect(mockPost).toHaveBeenCalledWith(
      '/api/entregas/10/calificar',
      { nota: 8.5, comentario: 'Buen trabajo' },
      { params: { profesorId: 7 } }
    );
  });

  it('realiza entrega con comentario y archivos en multipart/form-data', async () => {
    const fakeResponse = { data: { id: 1 } };
    mockPost.mockResolvedValue(fakeResponse);

    const archivo = new File(['contenido'], 'tarea.txt', { type: 'text/plain' });

    await entregaService.realizar(3, 9, 'Mi comentario', [archivo]);

    const [url, formData, config] = mockPost.mock.calls[0];

    expect(url).toBe('/api/entregas/entregable/3/estudiante/9');
    expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } });
    expect(formData).toBeInstanceOf(FormData);
    expect((formData as FormData).get('comentario')).toBe('Mi comentario');
    expect((formData as FormData).getAll('archivos')).toHaveLength(1);
  });

  it('realiza entrega sin comentario cuando viene vacio', async () => {
    const fakeResponse = { data: { id: 2 } };
    mockPost.mockResolvedValue(fakeResponse);

    await entregaService.realizar(5, 11, '   ', undefined);

    const [, formData] = mockPost.mock.calls[0];
    expect((formData as FormData).get('comentario')).toBeNull();
  });
});
