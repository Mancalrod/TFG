import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockPost } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
}));

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
  },
}));

import { oneDriveService } from '../oneDriveService';

describe('oneDriveService', () => {
  beforeEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    mockGet.mockReset();
    mockPost.mockReset();
  });

  it('isEnabled y getConnectionStatus consultan rutas correctas', async () => {
    mockGet
      .mockResolvedValueOnce({ data: { enabled: true } })
      .mockResolvedValueOnce({ data: { conectado: false } });

    const enabled = await oneDriveService.isEnabled();
    const status = await oneDriveService.getConnectionStatus(7);

    expect(enabled).toBe(true);
    expect(status).toEqual({ conectado: false });
    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/onedrive/enabled');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/onedrive/status/7');
  });

  it('listarCarpetas arma la URL con y sin parentId', async () => {
    mockGet.mockResolvedValue({ data: [] });

    await oneDriveService.listarCarpetas(5);
    await oneDriveService.listarCarpetas(5, 'parent-1');

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/onedrive/folders/5');
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/onedrive/folders/5?parentId=parent-1');
  });

  it('connectOneDrive resuelve true cuando llega mensaje de éxito', async () => {
    mockGet.mockResolvedValue({ data: { authUrl: 'https://microsoft.example/auth' } });

    const popup = { closed: false } as Window;
    vi.spyOn(globalThis, 'open').mockReturnValue(popup);

    const promise = oneDriveService.connectOneDrive(3);
    await vi.waitFor(() => expect(globalThis.open).toHaveBeenCalled());

    globalThis.dispatchEvent(
      new MessageEvent('message', {
        data: { type: 'onedrive-auth', success: true },
      })
    );

    await expect(promise).resolves.toBe(true);
    expect(mockGet).toHaveBeenCalledWith('/api/onedrive/auth-url/3');
    expect(globalThis.open).toHaveBeenCalledWith(
      'https://microsoft.example/auth',
      'onedrive-auth',
      'width=600,height=700,scrollbars=yes'
    );
  });

  it('connectOneDrive resuelve false si el popup se cierra', async () => {
    vi.useFakeTimers();
    mockGet.mockResolvedValue({ data: { authUrl: 'https://microsoft.example/auth' } });

    const popup = { closed: true } as Window;
    vi.spyOn(globalThis, 'open').mockReturnValue(popup);

    const promise = oneDriveService.connectOneDrive(8);
    await vi.advanceTimersByTimeAsync(1600);

    await expect(promise).resolves.toBe(false);
  });

  it('connectOneDrive ignora mensaje sin tipo esperado y termina en false al cerrar', async () => {
    vi.useFakeTimers();
    mockGet.mockResolvedValue({ data: { authUrl: 'https://microsoft.example/auth' } });

    const popupState = { closed: false };
    const popup = {
      get closed() {
        return popupState.closed;
      },
    } as Window;
    vi.spyOn(globalThis, 'open').mockReturnValue(popup);

    const promise = oneDriveService.connectOneDrive(12);
    await vi.waitFor(() => expect(globalThis.open).toHaveBeenCalled());

    globalThis.dispatchEvent(
      new MessageEvent('message', {
        data: { type: 'otro-evento', success: true },
      })
    );

    popupState.closed = true;
    await vi.advanceTimersByTimeAsync(1600);

    await expect(promise).resolves.toBe(false);
  });

  it('connectOneDrive devuelve false cuando llega mensaje onedrive-auth sin success true', async () => {
    mockGet.mockResolvedValue({ data: { authUrl: 'https://microsoft.example/auth' } });

    const popup = { closed: false } as Window;
    vi.spyOn(globalThis, 'open').mockReturnValue(popup);

    const promise = oneDriveService.connectOneDrive(6);
    await vi.waitFor(() => expect(globalThis.open).toHaveBeenCalled());

    globalThis.dispatchEvent(
      new MessageEvent('message', {
        data: { type: 'onedrive-auth', success: false },
      })
    );

    await expect(promise).resolves.toBe(false);
  });

  it('disconnectOneDrive usa endpoint esperado', async () => {
    mockPost.mockResolvedValue({});
    await oneDriveService.disconnectOneDrive(11);
    expect(mockPost).toHaveBeenCalledWith('/api/onedrive/disconnect/11');
  });
});