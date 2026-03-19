import { expect, test } from '@playwright/test';

const pendientes = [
  {
    entregaId: 101,
    cursoId: 10,
    cursoTitulo: 'Programación I',
    actividadId: 11,
    actividadTitulo: 'Actividad 1',
    entregableId: 12,
    entregableTitulo: 'Práctica Arrays',
    estudianteId: 200,
    estudianteNombre: 'Ana Pérez',
    estudianteCorreo: 'ana@demo.com',
    grupoTitulo: 'Grupo A',
    fechaEntrega: '2026-03-15T10:00:00Z',
    estado: 'ENTREGADO',
    fueATiempo: false,
    version: 2,
  },
  {
    entregaId: 102,
    cursoId: 10,
    cursoTitulo: 'Programación I',
    actividadId: 11,
    actividadTitulo: 'Actividad 1',
    entregableId: 13,
    entregableTitulo: 'Práctica Listas',
    estudianteId: 201,
    estudianteNombre: 'Luis Martín',
    estudianteCorreo: 'luis@demo.com',
    grupoTitulo: 'Grupo A',
    fechaEntrega: '2026-03-18T08:00:00Z',
    estado: 'ENTREGADO',
    fueATiempo: true,
    version: 1,
  },
];

test('flujo de evaluaciones con filtros y descarga mockeada', async ({ page }) => {
  let descargaActividadLlamada = 0;

  await page.addInitScript(() => {
    localStorage.setItem('accessToken', 'access-token-demo');
    localStorage.setItem('refreshToken', 'refresh-token-demo');
  });

  await page.route('**/api/auth/me', async (route) => {
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

  await page.route('**/api/entregas/profesor/1/pendientes', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(pendientes),
    });
  });

  await page.route('**/api/entregas/actividad/11/descargar-todo', async (route) => {
    descargaActividadLlamada += 1;
    await route.fulfill({
      status: 200,
      headers: {
        'Content-Type': 'application/zip',
        'Content-Disposition': 'attachment; filename="actividad_11.zip"',
      },
      body: 'zip-content',
    });
  });

  await page.goto('/evaluaciones');

  await expect(page.getByRole('heading', { name: 'Evaluaciones pendientes' })).toBeVisible();
  await expect(page.getByText('Programación I')).toBeVisible();

  await page.getByRole('button', { name: /Programación I/ }).first().click();
  await page.getByRole('button', { name: /Actividad 1/ }).first().click();

  const descargarBtn = page.getByRole('button', { name: /Descargar actividad/ });
  await expect(descargarBtn).toBeVisible();
  await descargarBtn.click();

  await expect.poll(() => descargaActividadLlamada).toBe(1);

  await page.getByRole('button', { name: 'Ver y calificar' }).first().click();
  await expect(page).toHaveURL(/\/entregas\/101$/);
});

