import { expect, test } from '@playwright/test';

test('login exitoso redirige al dashboard y muestra usuario', async ({ page }) => {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        accessToken: 'access-token-demo',
        refreshToken: 'refresh-token-demo',
        usuarioId: 1,
        nombre: 'Profesor Demo',
        correoElectronico: 'profesor@demo.com',
        roles: ['ROLE_PROFESOR'],
      }),
    });
  });

  // Cargas secundarias del dashboard.
  await page.route('**/api/cursos/profesor/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  await page.route('**/api/onedrive/status/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        conectado: false,
        microsoftEmail: null,
        fechaConexion: null,
      }),
    });
  });

  await page.goto('/login');

  await page.getByLabel('Correo electrónico').fill('profesor@demo.com');
  await page.getByLabel('Contraseña').fill('secreto');
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText('Microsoft OneDrive')).toBeVisible();
  await expect(page.getByText('Profesor Demo')).toBeVisible();
});

