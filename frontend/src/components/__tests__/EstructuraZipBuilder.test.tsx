import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import JSZip from 'jszip';
import EstructuraZipBuilder from '../EstructuraZipBuilder';

function renderBuilder(overrides = {}) {
  const props = {
    nodos: [],
    onChange: vi.fn(),
    estricta: false,
    onEstrictaChange: vi.fn(),
    nombreZipEsperado: '',
    onNombreZipChange: vi.fn(),
    ...overrides,
  };
  return { ...render(<EstructuraZipBuilder {...props} />), props };
}

async function crearZipDePrueba(): Promise<File> {
  const zip = new JSZip();
  zip.file('README.md', 'contenido');
  zip.file('src/Main.java', 'class Main {}');
  const buffer = await zip.generateAsync({ type: 'arraybuffer' });
  return new File([buffer], 'proyecto.zip', { type: 'application/zip' });
}

describe('EstructuraZipBuilder - Importar ZIP', () => {
  it('muestra la sección de importación', () => {
    renderBuilder();
    expect(screen.getByText('📥 Importar estructura desde ZIP')).toBeInTheDocument();
  });

  it('muestra las tres opciones de modo de importación', () => {
    renderBuilder();
    expect(screen.getByText('Nombres y extensiones')).toBeInTheDocument();
    expect(screen.getByText('Solo nombres')).toBeInTheDocument();
    expect(screen.getByText('Solo estructura')).toBeInTheDocument();
  });

  it('tiene seleccionado "Nombres y extensiones" por defecto', () => {
    renderBuilder();
    const radios = screen.getAllByRole('radio');
    const importRadios = radios.filter(
      r => (r as HTMLInputElement).name === 'modoImportacion'
    );
    expect((importRadios[0] as HTMLInputElement).checked).toBe(true);
  });

  it('permite cambiar el modo de importación', async () => {
    const user = userEvent.setup();
    renderBuilder();
    await user.click(screen.getByText('Solo estructura'));
    const radios = screen.getAllByRole('radio');
    const importRadios = radios.filter(
      r => (r as HTMLInputElement).name === 'modoImportacion'
    );
    expect((importRadios[2] as HTMLInputElement).checked).toBe(true);
  });

  it('muestra aviso cuando ya hay estructura definida', () => {
    renderBuilder({
      nodos: [{ id: '1', nombre: 'test', tipo: 'ARCHIVO', extensiones: ['txt'] }],
    });
    expect(screen.getByText('⚠️ Importar reemplazará la estructura actual')).toBeInTheDocument();
  });

  it('no muestra aviso cuando no hay estructura', () => {
    renderBuilder();
    expect(screen.queryByText('⚠️ Importar reemplazará la estructura actual')).not.toBeInTheDocument();
  });

  it('importa un ZIP y llama onChange con la estructura', async () => {
    const { props } = renderBuilder();
    const archivoZip = await crearZipDePrueba();

    const input = screen.getByTestId('ezb-import-file');
    fireEvent.change(input, { target: { files: [archivoZip] } });

    await waitFor(() => {
      expect(props.onChange).toHaveBeenCalled();
    });

    const nodosImportados = props.onChange.mock.calls[0][0];
    expect(nodosImportados.length).toBeGreaterThanOrEqual(2);

    const readme = nodosImportados.find(
      (n: { nombre: string }) => n.nombre === 'README'
    );
    expect(readme).toBeDefined();
  });

  it('muestra error con un archivo no-ZIP', async () => {
    const archivoFalso = new File(['no soy un zip'], 'falso.zip', {
      type: 'application/zip',
    });

    renderBuilder();

    const input = screen.getByTestId('ezb-import-file');
    fireEvent.change(input, { target: { files: [archivoFalso] } });

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
  });

  it('muestra el botón de importación con texto correcto', () => {
    renderBuilder();
    expect(screen.getByText('📂 Seleccionar ZIP')).toBeInTheDocument();
  });
});

describe('EstructuraZipBuilder - Funcionalidad interactiva existente', () => {
  it('permite añadir archivos manualmente', async () => {
    const user = userEvent.setup();
    const { props } = renderBuilder();
    await user.click(screen.getByText('+ 📄 Archivo'));
    expect(props.onChange).toHaveBeenCalled();
  });

  it('permite añadir carpetas manualmente', async () => {
    const user = userEvent.setup();
    const { props } = renderBuilder();
    await user.click(screen.getByText('+ 📁 Carpeta'));
    expect(props.onChange).toHaveBeenCalled();
  });

  it('muestra modo de validación', () => {
    renderBuilder();
    expect(screen.getByText('Mínimo requerido')).toBeInTheDocument();
    expect(screen.getByText('Estructura exacta')).toBeInTheDocument();
  });

  it('muestra el campo de nombre del ZIP', () => {
    renderBuilder();
    expect(screen.getByLabelText('Nombre del archivo ZIP esperado')).toBeInTheDocument();
  });
});
