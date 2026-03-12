import { describe, it, expect } from 'vitest';
import { construirArbolDesdeEntradas, ModoImportacion } from '../zipStructureParser';

interface EntradaZip {
  ruta: string;
  esCarpeta: boolean;
}

function crearEntradas(rutas: string[]): EntradaZip[] {
  return rutas.map(ruta => ({
    ruta,
    esCarpeta: ruta.endsWith('/'),
  }));
}

describe('construirArbolDesdeEntradas', () => {
  describe('modo nombres_extensiones', () => {
    const modo: ModoImportacion = 'nombres_extensiones';

    it('maneja un ZIP vacío', () => {
      const resultado = construirArbolDesdeEntradas([], modo);
      expect(resultado).toEqual([]);
    });

    it('parsea archivos en la raíz', () => {
      const entradas = crearEntradas(['main.java', 'README.md']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(2);
      expect(resultado[0].nombre).toBe('main');
      expect(resultado[0].tipo).toBe('ARCHIVO');
      expect(resultado[0].extensiones).toEqual(['java']);
      expect(resultado[1].nombre).toBe('README');
      expect(resultado[1].extensiones).toEqual(['md']);
    });

    it('parsea carpetas con archivos dentro', () => {
      const entradas = crearEntradas([
        'src/',
        'src/Main.java',
        'src/Utils.py',
      ]);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(1);
      expect(resultado[0].nombre).toBe('src');
      expect(resultado[0].tipo).toBe('CARPETA');
      expect(resultado[0].hijos).toHaveLength(2);
      expect(resultado[0].hijos![0].nombre).toBe('Main');
      expect(resultado[0].hijos![0].extensiones).toEqual(['java']);
    });

    it('parsea estructura anidada profunda', () => {
      const entradas = crearEntradas([
        'proyecto/',
        'proyecto/src/',
        'proyecto/src/main/',
        'proyecto/src/main/App.java',
      ]);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(1);
      const proyecto = resultado[0];
      expect(proyecto.nombre).toBe('proyecto');
      expect(proyecto.hijos![0].nombre).toBe('src');
      expect(proyecto.hijos![0].hijos![0].nombre).toBe('main');
      expect(proyecto.hijos![0].hijos![0].hijos![0].nombre).toBe('App');
      expect(proyecto.hijos![0].hijos![0].hijos![0].extensiones).toEqual(['java']);
    });

    it('maneja archivos sin extensión', () => {
      const entradas = crearEntradas(['Makefile', 'Dockerfile']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(2);
      expect(resultado[0].nombre).toBe('Dockerfile');
      expect(resultado[0].extensiones).toEqual([]);
      expect(resultado[1].nombre).toBe('Makefile');
      expect(resultado[1].extensiones).toEqual([]);
    });

    it('maneja archivos con punto al inicio (ocultos)', () => {
      const entradas = crearEntradas(['.gitignore', '.env']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(2);
      expect(resultado[0].nombre).toBe('.env');
      expect(resultado[0].extensiones).toEqual([]);
    });

    it('crea carpetas implícitas cuando no hay entrada de directorio', () => {
      const entradas = crearEntradas([
        'src/main/App.java',
      ]);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(1);
      expect(resultado[0].nombre).toBe('src');
      expect(resultado[0].tipo).toBe('CARPETA');
      expect(resultado[0].hijos![0].nombre).toBe('main');
      expect(resultado[0].hijos![0].tipo).toBe('CARPETA');
      expect(resultado[0].hijos![0].hijos![0].nombre).toBe('App');
    });

    it('no duplica carpetas existentes', () => {
      const entradas = crearEntradas([
        'src/',
        'src/A.java',
        'src/B.java',
      ]);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(1);
      expect(resultado[0].hijos).toHaveLength(2);
    });

    it('convierte extensiones a minúsculas', () => {
      const entradas = crearEntradas(['readme.MD', 'Main.JAVA']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado[0].extensiones).toEqual(['java']);
      expect(resultado[1].extensiones).toEqual(['md']);
    });
  });

  describe('modo solo_nombres', () => {
    const modo: ModoImportacion = 'solo_nombres';

    it('mantiene nombres pero ignora extensiones', () => {
      const entradas = crearEntradas(['main.java', 'README.md']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(2);
      expect(resultado[0].nombre).toBe('main');
      expect(resultado[0].extensiones).toEqual([]);
      expect(resultado[1].nombre).toBe('README');
      expect(resultado[1].extensiones).toEqual([]);
    });

    it('mantiene nombres de carpetas normalmente', () => {
      const entradas = crearEntradas(['src/', 'src/Main.java']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado[0].nombre).toBe('src');
      expect(resultado[0].hijos![0].nombre).toBe('Main');
      expect(resultado[0].hijos![0].extensiones).toEqual([]);
    });
  });

  describe('modo solo_estructura', () => {
    const modo: ModoImportacion = 'solo_estructura';

    it('usa comodín para nombres y conserva extensiones', () => {
      const entradas = crearEntradas(['main.java', 'README.md']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado).toHaveLength(2);
      expect(resultado[0].nombre).toBe('*');
      expect(resultado[0].extensiones).toEqual(['java']);
      expect(resultado[1].nombre).toBe('*');
      expect(resultado[1].extensiones).toEqual(['md']);
    });

    it('usa comodín para nombres de carpetas', () => {
      const entradas = crearEntradas(['src/', 'src/Main.java']);
      const resultado = construirArbolDesdeEntradas(entradas, modo);

      expect(resultado[0].nombre).toBe('*');
      expect(resultado[0].tipo).toBe('CARPETA');
      expect(resultado[0].hijos![0].nombre).toBe('*');
      expect(resultado[0].hijos![0].extensiones).toEqual(['java']);
    });
  });

  describe('filtrado de entradas del sistema', () => {
    it('ignora rutas vacías', () => {
      const entradas = crearEntradas(['']);
      const resultado = construirArbolDesdeEntradas(entradas, 'nombres_extensiones');
      expect(resultado).toEqual([]);
    });
  });

  describe('genera IDs únicos', () => {
    it('cada nodo tiene un ID', () => {
      const entradas = crearEntradas(['a.txt', 'b.txt', 'src/', 'src/c.java']);
      const resultado = construirArbolDesdeEntradas(entradas, 'nombres_extensiones');

      const ids = new Set<string>();
      function recogerIds(nodos: { id: string; hijos?: { id: string; hijos?: unknown[] }[] }[]) {
        for (const nodo of nodos) {
          expect(nodo.id).toBeTruthy();
          ids.add(nodo.id);
          if ('hijos' in nodo && nodo.hijos) {
            recogerIds(nodo.hijos as { id: string; hijos?: { id: string; hijos?: unknown[] }[] }[]);
          }
        }
      }
      recogerIds(resultado);

      expect(ids.size).toBe(4);
    });
  });

  describe('estructura mixta', () => {
    it('maneja archivos y carpetas mezclados en la raíz', () => {
      const entradas = crearEntradas([
        'README.md',
        'src/',
        'src/index.ts',
        'package.json',
        'docs/',
        'docs/guide.md',
      ]);
      const resultado = construirArbolDesdeEntradas(entradas, 'nombres_extensiones');

      const nombres = resultado.map(n => n.nombre);
      expect(nombres).toContain('README');
      expect(nombres).toContain('src');
      expect(nombres).toContain('package');
      expect(nombres).toContain('docs');

      const src = resultado.find(n => n.nombre === 'src');
      expect(src?.hijos).toHaveLength(1);
      expect(src?.hijos![0].nombre).toBe('index');
      expect(src?.hijos![0].extensiones).toEqual(['ts']);
    });
  });
});
