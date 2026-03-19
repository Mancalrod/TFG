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

## 5. Resultados verificados (2026-03-19)

### Frontend E2E

- Comando: `npm run test:e2e`
- Resultado: `2 passed (5.9s)`
- Specs ejecutadas:
	- `frontend/e2e/login.spec.ts`
	- `frontend/e2e/evaluaciones.spec.ts`

### Frontend mutacion (Stryker)

- Comando: `npm run test:mutation`
- Resultado global: `64.26%`
- Umbral `break`: `50` (en verde)
- Detalle principal:
	- `services`: `51.63%`
	- `utils/zipStructureParser.ts`: `93.67%`

### Frontend coverage (unitarias)

- Ultimo reporte disponible en `frontend/coverage/index.html`:
	- Statements: `11.85%`
	- Branches: `9.03%`
	- Functions: `12.25%`
	- Lines: `12.35%`

### Backend

- En esta revision no se relanzo `mvnw verify` ni PIT manualmente.
- Se mantiene la estrategia y rutas de reporte documentadas en este archivo.

## 6. Criterios recomendados de aceptacion

- `lint` sin errores.
- unit/integracion/E2E en verde.
- cobertura frontend y backend estable o creciente.
- score de mutacion por encima de umbral (`break`) y tendencia ascendente.

