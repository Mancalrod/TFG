import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: [
        'src/App.tsx',
        'src/context/**/*.{ts,tsx}',
        'src/services/**/*.ts',
        'src/utils/**/*.ts',
        'src/components/Navbar.tsx',
        'src/pages/login/LoginPage.tsx'
      ],
      exclude: [
        'src/**/*.d.ts',
        'src/main.tsx',
        'src/vite-env.d.ts',
        'src/test/**',
        'src/**/__tests__/**',
        'src/services/api.ts',
        'src/services/index.ts'
      ],
      thresholds: {
        lines: 95,
        functions: 95,
        statements: 95,
        branches: 85
      }
    }
  },
});
