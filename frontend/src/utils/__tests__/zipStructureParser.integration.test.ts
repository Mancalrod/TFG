import { describe, it, expect } from 'vitest';
import JSZip from 'jszip';
import { parsearEstructuraZip } from '../zipStructureParser';

function crearArchivoDesdeBuffer(buffer: ArrayBuffer, nombre: string): File {
  return new File([buffer], nombre, { type: 'application/zip' });
}

describe('parsearEstructuraZip (integración con JSZip)', () => {
  it('parsea un ZIP real con archivos y carpetas', async () => {
    const zip = new JSZip();
    zip.file('README.md', 'contenido');
    zip.file('src/Main.java', 'class Main {}');
    zip.file('src/utils/Helper.java', 'class Helper {}');

    const buffer = await zip.generateAsync({ type: 'arraybuffer' });
    const archivo = crearArchivoDesdeBuffer(buffer, 'proyecto.zip');

    const resultado = await parsearEstructuraZip(archivo, 'nombres_extensiones');

    expect(resultado.length).toBeGreaterThanOrEqual(2);

    const readme = resultado.find(n => n.nombre === 'README');
    expect(readme).toBeDefined();
    expect(readme!.extensiones).toEqual(['md']);

    const src = resultado.find(n => n.nombre === 'src');
    expect(src).toBeDefined();
    expect(src!.tipo).toBe('CARPETA');

    const mainFile = src!.hijos!.find(n => n.nombre === 'Main');
    expect(mainFile).toBeDefined();
    expect(mainFile!.extensiones).toEqual(['java']);
  });

  it('aplica modo solo_estructura correctamente', async () => {
    const zip = new JSZip();
    zip.file('app/index.ts', 'export default {}');
    zip.file('app/styles.css', 'body {}');

    const buffer = await zip.generateAsync({ type: 'arraybuffer' });
    const archivo = crearArchivoDesdeBuffer(buffer, 'test.zip');

    const resultado = await parsearEstructuraZip(archivo, 'solo_estructura');

    expect(resultado).toHaveLength(1);
    expect(resultado[0].nombre).toBe('*');
    expect(resultado[0].tipo).toBe('CARPETA');
    expect(resultado[0].hijos!.every(h => h.nombre === '*')).toBe(true);

    const extensiones = resultado[0].hijos!.map(h => h.extensiones![0]);
    expect(extensiones).toContain('ts');
    expect(extensiones).toContain('css');
  });

  it('aplica modo solo_nombres correctamente', async () => {
    const zip = new JSZip();
    zip.file('config.yaml', 'key: value');

    const buffer = await zip.generateAsync({ type: 'arraybuffer' });
    const archivo = crearArchivoDesdeBuffer(buffer, 'test.zip');

    const resultado = await parsearEstructuraZip(archivo, 'solo_nombres');

    expect(resultado).toHaveLength(1);
    expect(resultado[0].nombre).toBe('config');
    expect(resultado[0].extensiones).toEqual([]);
  });

  it('filtra archivos de sistema macOS', async () => {
    const zip = new JSZip();
    zip.file('main.py', 'print("hello")');
    zip.file('__MACOSX/main.py', '');
    zip.file('.DS_Store', '');

    const buffer = await zip.generateAsync({ type: 'arraybuffer' });
    const archivo = crearArchivoDesdeBuffer(buffer, 'test.zip');

    const resultado = await parsearEstructuraZip(archivo, 'nombres_extensiones');

    const nombres = resultado.map(n => n.nombre);
    expect(nombres).not.toContain('__MACOSX');
    expect(nombres).toContain('main');
  });

  it('rechaza un archivo no-ZIP', async () => {
    const contenido = new TextEncoder().encode('esto no es un zip');
    const archivo = new File([contenido], 'falso.zip', { type: 'application/zip' });

    await expect(parsearEstructuraZip(archivo, 'nombres_extensiones'))
      .rejects.toThrow();
  });

  it('maneja ZIP con carpetas vacías', async () => {
    const zip = new JSZip();
    zip.folder('empty-folder');
    zip.file('root.txt', 'contenido');

    const buffer = await zip.generateAsync({ type: 'arraybuffer' });
    const archivo = crearArchivoDesdeBuffer(buffer, 'test.zip');

    const resultado = await parsearEstructuraZip(archivo, 'nombres_extensiones');

    const carpeta = resultado.find(n => n.tipo === 'CARPETA');
    expect(carpeta).toBeDefined();
    expect(carpeta!.nombre).toBe('empty-folder');
    expect(carpeta!.hijos).toEqual([]);
  });
});
