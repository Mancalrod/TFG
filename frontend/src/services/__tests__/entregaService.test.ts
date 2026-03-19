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

  it('obtener y listados usan rutas esperadas', async () => {
    mockGet.mockResolvedValue({ data: [] });

    await entregaService.obtener(8);
    await entregaService.listarParaEvaluar(11);
    await entregaService.listarHistorial(11, 9);
    await entregaService.listarPorEstudiante(9);
    await entregaService.listarPendientesCalificar(3);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/entregas/8');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/entregas/entregable/11');
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/entregas/entregable/11/estudiante/9');
    expect(mockGet).toHaveBeenNthCalledWith(4, '/api/entregas/estudiante/9');
    expect(mockGet).toHaveBeenNthCalledWith(5, '/api/entregas/profesor/3/pendientes');
  });

  it('descarga y previsualizacion solicitan blob', async () => {
    const blob = new Blob(['x']);
    mockGet.mockResolvedValue({ data: blob });

    await entregaService.descargarArchivo(20);
    await entregaService.previsualizarArchivo(20);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/entregas/archivo/20', {
      responseType: 'blob',
    });
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/entregas/archivo/20/preview', {
      responseType: 'blob',
    });
  });

  it('descargarTodo usa filename del header si existe', async () => {
    const blob = new Blob(['zip']);
    mockGet.mockResolvedValue({
      data: blob,
      headers: {
        'content-disposition': 'attachment; filename="entregas_final.zip"',
      },
    });

    const result = await entregaService.descargarTodo(15);

    expect(mockGet).toHaveBeenCalledWith('/api/entregas/entregable/15/descargar-todo', {
      responseType: 'blob',
    });
    expect(result.filename).toBe('entregas_final.zip');
  });

  it('descargarTodo y descargarTodoActividad usan fallback de filename', async () => {
    const blob = new Blob(['zip']);
    mockGet
      .mockResolvedValueOnce({ data: blob, headers: {} })
      .mockResolvedValueOnce({ data: blob, headers: {} });

    const entregaZip = await entregaService.descargarTodo(15);
    const actividadZip = await entregaService.descargarTodoActividad(6);

    expect(entregaZip.filename).toBe('entregas_15.zip');
    expect(actividadZip.filename).toBe('actividad_6.zip');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/entregas/actividad/6/descargar-todo', {
      responseType: 'blob',
    });
  });

  it('listarContenidoZip, estadisticas y eliminar usan endpoints correctos', async () => {
    mockGet
      .mockResolvedValueOnce({ data: [{ nombre: 'a.txt', tamano: 1, esCarpeta: false }] })
      .mockResolvedValueOnce({ data: { total: 2 } });
    mockDelete.mockResolvedValue({});

    await entregaService.listarContenidoZip(30);
    await entregaService.obtenerEstadisticas(40);
    await entregaService.eliminar(50);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/entregas/archivo/30/zip-contenido');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/entregas/entregable/40/estadisticas');
    expect(mockDelete).toHaveBeenCalledWith('/api/entregas/50');
  });
});
