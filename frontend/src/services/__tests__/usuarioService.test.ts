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

import { usuarioService } from '../usuarioService';

describe('usuarioService', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
  });

  it('consultas GET usan rutas correctas', async () => {
    mockGet.mockResolvedValue({ data: [] });

    await usuarioService.listar();
    await usuarioService.obtener(3);
    await usuarioService.obtenerPorCorreo('demo@test.com');
    await usuarioService.esProfesor(3);
    await usuarioService.esEstudiante(3);
    await usuarioService.listarEstudiantesDeGrupo(44);
    await usuarioService.obtenerProfesorId(3);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/usuarios');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/usuarios/3');
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/usuarios/correo/demo@test.com');
    expect(mockGet).toHaveBeenNthCalledWith(4, '/api/usuarios/3/es-profesor');
    expect(mockGet).toHaveBeenNthCalledWith(5, '/api/usuarios/3/es-estudiante');
    expect(mockGet).toHaveBeenNthCalledWith(6, '/api/usuarios/grupo/44');
    expect(mockGet).toHaveBeenNthCalledWith(7, '/api/usuarios/3/profesor-id');
  });

  it('crear y actualizar usan endpoints esperados', async () => {
    const payload = { nombre: 'Ana' } as never;
    mockPost.mockResolvedValue({ data: { id: 1 } });
    mockPut.mockResolvedValue({ data: { id: 2 } });

    await usuarioService.crear(payload);
    await usuarioService.actualizar(2, payload);

    expect(mockPost).toHaveBeenCalledWith('/api/usuarios', payload);
    expect(mockPut).toHaveBeenCalledWith('/api/usuarios/2', payload);
  });

  it('acciones de alta/baja de roles y grupos usan rutas correctas', async () => {
    mockPost.mockResolvedValue({});
    mockDelete.mockResolvedValue({});

    await usuarioService.registrarComoProfesor(9);
    await usuarioService.registrarComoEstudiante(9, 4);
    await usuarioService.eliminar(9);
    await usuarioService.eliminarRolProfesor(9);
    await usuarioService.eliminarRolEstudiante(9);
    await usuarioService.eliminarEstudianteDeGrupo(9, 4);

    expect(mockPost).toHaveBeenNthCalledWith(1, '/api/usuarios/9/profesor');
    expect(mockPost).toHaveBeenNthCalledWith(2, '/api/usuarios/9/estudiante/4');

    expect(mockDelete).toHaveBeenNthCalledWith(1, '/api/usuarios/9');
    expect(mockDelete).toHaveBeenNthCalledWith(2, '/api/usuarios/9/profesor');
    expect(mockDelete).toHaveBeenNthCalledWith(3, '/api/usuarios/9/estudiante');
    expect(mockDelete).toHaveBeenNthCalledWith(4, '/api/usuarios/9/estudiante/4');
  });
});