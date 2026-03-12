import JSZip from 'jszip';
import { NodoEstructuraZip } from '../types';

export type ModoImportacion = 'nombres_extensiones' | 'solo_nombres' | 'solo_estructura';

const generarId = () => `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;

interface EntradaZip {
  ruta: string;
  esCarpeta: boolean;
}

function extraerEntradasZip(zip: JSZip): EntradaZip[] {
  const entradas: EntradaZip[] = [];
  zip.forEach((rutaRelativa, entrada) => {
    if (rutaRelativa.startsWith('__MACOSX') || rutaRelativa.includes('.DS_Store')) {
      return;
    }
    entradas.push({
      ruta: rutaRelativa,
      esCarpeta: entrada.dir,
    });
  });
  return entradas;
}

function separarNombreExtension(nombreArchivo: string): { nombre: string; extension: string } {
  const ultimoPunto = nombreArchivo.lastIndexOf('.');
  if (ultimoPunto <= 0) {
    return { nombre: nombreArchivo, extension: '' };
  }
  return {
    nombre: nombreArchivo.slice(0, ultimoPunto),
    extension: nombreArchivo.slice(ultimoPunto + 1).toLowerCase(),
  };
}

function crearNodoDesdeEntrada(
  nombreSegmento: string,
  esCarpeta: boolean,
  modo: ModoImportacion
): NodoEstructuraZip {
  if (esCarpeta) {
    const nombre = modo === 'solo_estructura' ? '*' : nombreSegmento;
    return {
      id: generarId(),
      nombre,
      tipo: 'CARPETA',
      extensiones: [],
      hijos: [],
    };
  }

  const { nombre, extension } = separarNombreExtension(nombreSegmento);

  switch (modo) {
    case 'nombres_extensiones':
      return {
        id: generarId(),
        nombre,
        tipo: 'ARCHIVO',
        extensiones: extension ? [extension] : [],
      };
    case 'solo_nombres':
      return {
        id: generarId(),
        nombre,
        tipo: 'ARCHIVO',
        extensiones: [],
      };
    case 'solo_estructura':
      return {
        id: generarId(),
        nombre: '*',
        tipo: 'ARCHIVO',
        extensiones: extension ? [extension] : [],
      };
  }
}

function encontrarOCrearCarpeta(
  nodos: NodoEstructuraZip[],
  nombre: string,
  modo: ModoImportacion
): NodoEstructuraZip {
  const nombreBuscar = modo === 'solo_estructura' ? '*' : nombre;
  const existente = nodos.find(
    n => n.tipo === 'CARPETA' && n.nombre === nombreBuscar
  );
  if (existente) {
    return existente;
  }
  const nuevo = crearNodoDesdeEntrada(nombre, true, modo);
  nodos.push(nuevo);
  return nuevo;
}

export function construirArbolDesdeEntradas(
  entradas: EntradaZip[],
  modo: ModoImportacion
): NodoEstructuraZip[] {
  const raiz: NodoEstructuraZip[] = [];

  const entradasOrdenadas = [...entradas].sort((a, b) => a.ruta.localeCompare(b.ruta));

  for (const entrada of entradasOrdenadas) {
    const segmentos = entrada.ruta.split('/').filter(s => s.length > 0);
    if (segmentos.length === 0) continue;

    let nodosActuales = raiz;

    for (let i = 0; i < segmentos.length; i++) {
      const esUltimo = i === segmentos.length - 1;
      const segmento = segmentos[i];

      if (esUltimo && !entrada.esCarpeta) {
        const nuevoArchivo = crearNodoDesdeEntrada(segmento, false, modo);
        nodosActuales.push(nuevoArchivo);
      } else {
        const carpeta = encontrarOCrearCarpeta(nodosActuales, segmento, modo);
        nodosActuales = carpeta.hijos!;
      }
    }
  }

  return raiz;
}

export async function parsearEstructuraZip(
  archivo: File,
  modo: ModoImportacion
): Promise<NodoEstructuraZip[]> {
  const buffer = await archivo.arrayBuffer();
  const zip = await JSZip.loadAsync(buffer);
  const entradas = extraerEntradasZip(zip);
  return construirArbolDesdeEntradas(entradas, modo);
}
