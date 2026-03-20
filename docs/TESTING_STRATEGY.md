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

## 5. Resultados verificados (2026-03-20)

### Frontend E2E

- Comando: `npm run test:e2e`
- Resultado: `3 passed (12.1s)`
- Specs ejecutadas:
	- `frontend/e2e/login.spec.ts`
	- `frontend/e2e/evaluaciones.spec.ts`
	- Nuevo escenario: login fallido con mensaje de credenciales incorrectas

### Frontend mutacion (Stryker)

- Comando: `npm run test:mutation`
- Resultado global: `95.42%`
- Umbral `break`: `50` (en verde)
- Detalle principal:
	- `services`: `96.17%`
	- `utils/zipStructureParser.ts`: `93.67%`

### Frontend coverage (unitarias)

- Comando: `npm run test:coverage`
- Ultimo reporte en `frontend/coverage/index.html`:
	- Statements: `98.66%`
	- Branches: `86.25%`
	- Functions: `98.44%`
	- Lines: `98.63%`
	- Umbrales configurados en Vitest:
		- Statements: `95%`
		- Functions: `95%`
		- Lines: `95%`
		- Branches: `85%`
	- Cobertura destacada por modulo:
		- `src/services`: `99.50%` statements
		- `src/utils/zipStructureParser.ts`: `100%` statements
		- `src/context`: `98.30%` statements
		- `src/pages/login/LoginPage.tsx`: `97.14%` statements
		- `src/components/Navbar.tsx`: `100%` statements

### Backend

- Comandos ejecutados:
	- `./mvnw -Dtest=EntregaServiceTest test`
	- `./mvnw -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage`
- Resultado PIT global:
	- Mutations generated: `1225`
	- Mutations killed: `915`
	- Mutation score: `75%`
	- Line coverage (clases mutadas): `99%`
	- Test strength: `76%`
- Resultado tests objetivo:
	- `EntregaServiceTest`: `87 tests`, `0 failures`, `0 errors`

## 6. Criterios recomendados de aceptacion

- `lint` sin errores.
- unit/integracion/E2E en verde.
- cobertura frontend y backend estable o creciente.
- score de mutacion por encima de umbral (`break`) y tendencia ascendente.

