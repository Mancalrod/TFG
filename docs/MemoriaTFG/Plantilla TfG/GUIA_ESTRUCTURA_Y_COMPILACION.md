# Guia de la plantilla TfG (estructura y compilacion)

Este documento explica como esta organizada la plantilla de LaTeX en esta carpeta y como rellenarla con vuestros datos.

## 1) Archivo principal

El archivo que controla todo es:

- proyect.tex

En ese archivo se define:

1. Clase y configuracion general.
2. Datos de portada.
3. Orden de las partes del documento.
4. Capitulo que se incluye.
5. Estilo y base de datos de bibliografia.

## 2) Estructura logica del documento

En proyect.tex la estructura va en este orden:

1. Portada
   - Comandos de datos: tipo, titulopro, tutor, departamento, autores, dia, titulacion.
   - Se genera con hacerportada.
2. Front matter (preliminares)
   - input resumen
   - input agradecimientos
   - tableofcontents
   - listoftables
   - listoffigures
   - lstlistoflistings
3. Main matter (cuerpo)
   - input Capitulos/capitulo1
   - input Capitulos/capitulo2
   - y otros capitulos opcionales que estan comentados
4. Back matter (parte final)
   - apendices (opcional)
   - bibliographystyle
   - bibliography

## 3) Que carpeta sirve para cada cosa

- Capitulos/
  - Aqui va el contenido principal de la memoria (capitulos).
- img/
  - Aqui van imagenes y figuras.
- codigo/
  - Fragmentos de codigo externos (por ejemplo para incluir listings).
- pfcbib.bib
  - Base de datos BibTeX con las referencias bibliograficas.
- pclass.cls y ficheros .sty/.bst
  - Soporte de formato de la plantilla. Normalmente no se tocan.

## 4) Que teneis que rellenar vosotros

### 4.1 Portada y datos academicos

Editar en proyect.tex:

- tipo
- titulopro
- tutor
- departamento
- autores
- dia
- titulacion

### 4.2 Resumen y agradecimientos

Editar:

- resumen.tex
- agradecimientos.tex

### 4.3 Contenido de capitulos

Editar o crear archivos dentro de Capitulos/.

Si quereis activar capitulos que ahora estan comentados en proyect.tex, quitad el simbolo % en las lineas input correspondientes.

### 4.4 Bibliografia

1. Anadid entradas en pfcbib.bib.
2. Citad en el texto con cite{clave}.
3. Dejad en proyect.tex:
   - bibliographystyle{apacite}
   - bibliography{pfcbib}

## 5) Como se construye el PDF

## Opcion A: VS Code (LaTeX Workshop)

1. Abrir proyect.tex.
2. Ejecutar Build LaTeX project.
3. Ver el PDF generado.

## Opcion B: Terminal con latexmk

Desde la carpeta Plantilla TfG:

latexmk -pdf -interaction=nonstopmode proyect.tex

## Opcion C: Secuencia manual clasica

Si no usais latexmk:

1. pdflatex proyect.tex
2. bibtex proyect
3. pdflatex proyect.tex
4. pdflatex proyect.tex

La 2a y 3a compilacion son necesarias para resolver citas, referencias e indices.

## 6) Archivos generados al compilar

Es normal que aparezcan archivos auxiliares como:

- proyect.aux
- proyect.bbl
- proyect.log
- proyect.toc
- proyect.lof
- proyect.lot
- proyect.lol
- proyect.fls
- proyect.fdb_latexmk
- proyect.synctex.gz

Y el resultado final:

- proyect.pdf

## 7) Flujo recomendado (paso a paso)

1. Rellenar datos de portada en proyect.tex.
2. Escribir resumen.tex y agradecimientos.tex.
3. Completar capitulos en Capitulos/.
4. Anadir referencias en pfcbib.bib y citar en el texto.
5. Compilar con latexmk.
6. Repetir compilacion hasta que desaparezcan avisos de referencias no resueltas.

## 8) Nota practica sobre codificacion

En proyect.tex aparece inputenc en UTF-8 (y una linea alternativa para latin1 comentada). Manteneos en un solo formato de codificacion en todo el proyecto para evitar caracteres raros.
