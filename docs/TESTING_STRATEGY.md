# Estrategia de testing (hardening final)

Este documento define una estrategia de pruebas en capas para validar la aplicacion de extremo a extremo, con foco en regresiones, seguridad funcional y calidad del diseño de tests.

## 1. Piramide de pruebas

- Unitarias (rapidas): logica de servicios, utilidades, mapeos, validaciones.
- Integracion: controladores/API con `MockMvc`, servicios con `MockWebServer`, persistencia y seguridad.
- E2E mockeadas: flujos completos de UI en navegador con API simulada (sin backend real).
- Mutacion: calidad de tests frente a cambios de comportamiento (PIT/Stryker).

## 2. Backend

### Ejecucion habitual

```bash
cd backend
./mvnw verify
```

### Cobertura

JaCoCo se ejecuta en `test` y genera reporte en:

- `backend/target/site/jacoco/index.html`

### Mutacion (PIT)

Configurado en perfil Maven `mutation` para paquetes de `service` y `controller`.

```bash
cd backend
./mvnw -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
```

Salida:

- `backend/target/pit-reports/`

## 3. Frontend

### Lint + unitarias + coverage

```bash
cd frontend
npm run test:ci
```

Comandos individuales:

```bash
npm run lint
npm run test
npm run test:coverage
```

### E2E mockeadas (Playwright)

Las pruebas E2E levantan Vite y mockean `/api/**` con `page.route`, por lo que no necesitan backend activo.

```bash
cd frontend
npx playwright install chromium
npm run test:e2e
```

### Mutacion (Stryker)

```bash
cd frontend
npm run test:mutation
```

Salida:

- `frontend/reports/mutation/mutation.html`

## 4. CI/CD

- `frontend-ci.yml`: lint + unitarias (coverage) + E2E mockeadas + build.
- `full-stack-ci.yml`: backend verify + frontend quality completa.
- `mutation-ci.yml`: mutacion backend (PIT) y frontend (Stryker), manual o planificada.

### 4.1 Validacion de workflows

- Los workflows de frontend y full-stack ejecutan los pasos documentados (lint, coverage, E2E mockeadas y build).
- `mutation-ci.yml` ejecuta PIT en backend y Stryker en frontend, y sube artifacts de ambos reportes.
- La configuracion de Stryker se ajusto para mutar codigo de produccion y excluir tests (`__tests__`, `*.test.*`, `*.spec.*`).

## 5. Resultados verificados (2026-04-06)

### Frontend E2E

- Comando: `npm run test:e2e`
- Resultado: `3 passed (5.7s)`
- Specs ejecutadas:
	- `frontend/e2e/login.spec.ts`
	- `frontend/e2e/evaluaciones.spec.ts`
	- Escenarios validados: login exitoso, login fallido y flujo de evaluaciones con filtros/descarga mockeada

Nota: durante E2E aparecen logs de proxy `ECONNREFUSED` para endpoints no mockeados de notificaciones; no afectan al resultado final de los tests.

### Frontend mutacion (Stryker)

- Comando: `npm run test:mutation`
- Resultado global: `98.61%`
- Umbral `break`: `50` (en verde)
- Detalle principal:
	- `services`: `100.00%`
	- `utils/zipStructureParser.ts`: `94.94%`
	- Mutantes instrumentados: `431` (`124` killed, `160` timeout, `4` survived)

### Frontend coverage (unitarias)

- Comando: `npm run test:coverage`
- Ultimo reporte en `frontend/coverage/index.html`:
	- Statements: `97.77%`
	- Branches: `90.68%`
	- Functions: `98.19%`
	- Lines: `98.25%`
	- Total tests: `137 passed (21 files)`
	- Umbrales configurados en Vitest:
		- Statements: `95%`
		- Functions: `95%`
		- Lines: `95%`
		- Branches: `85%`
	- Cobertura destacada por modulo:
		- `src/services`: `97.89%` statements
		- `src/utils/zipStructureParser.ts`: `100%` statements
		- `src/context`: `100%` statements
		- `src/pages/login/LoginPage.tsx`: `100%` statements
		- `src/components/Navbar.tsx`: `95%` statements

### Backend

- Comandos ejecutados:
	- `./mvnw -Dtest=EntregaServiceTest test --batch-mode --no-transfer-progress`
	- `./mvnw -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage --batch-mode --no-transfer-progress`
- Resultado PIT global:
	- Mutations generated: `143`
	- Mutations killed: `133`
	- Mutation score: `93%`
	- Line coverage (clases mutadas): `95%`
	- Test strength: `96%`
	- Nota de alcance: el perfil `mutation` actual usa `targetClasses` explicitas para los modulos mas estables en mutacion.
- Resultado tests objetivo:
	- `EntregaServiceTest`: `89 tests`, `0 failures`, `0 errors`

## 6. Criterios recomendados de aceptacion

- `lint` sin errores.
- unit/integracion/E2E en verde.
- cobertura frontend y backend estable o creciente.
- score de mutacion por encima de umbral (`break`) y tendencia ascendente.

