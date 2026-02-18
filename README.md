# Sistema de Gestión de Entregables

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18+-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)

> Trabajo de Fin de Grado - Sistema de gestión de entregables académicos

## 📋 Descripción

Este proyecto implementa un sistema de gestión de entregables que permite [descripción breve del propósito del sistema].

## 🏗️ Arquitectura

El proyecto sigue una arquitectura cliente-servidor con las siguientes tecnologías:

### Backend
- **Java 17+** - Lenguaje de programación
- **Spring Boot 3.x** - Framework de desarrollo
- **Maven** - Gestión de dependencias y build

### Frontend
- **React 18+** - Biblioteca de UI
- **TypeScript** - Tipado estático
- **Node.js** - Entorno de ejecución

## 📁 Estructura del Proyecto

```
TFG/
├── backend/                 # API REST con Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
├── frontend/                # Aplicación React
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── types/
│   ├── package.json
│   └── tsconfig.json
├── docs/                    # Documentación
│   ├── ERS TFG.pdf
│   └── DAS TFG.pdf
└── README.md
```

## 🚀 Instalación y Ejecución

### Prerrequisitos

- Java JDK 17+
- Maven 3.8+
- Node.js 18+
- npm o yarn

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

El servidor estará disponible en `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm start
```

La aplicación estará disponible en `http://localhost:3000`

## 📖 Documentación

- [ERS - Especificación de Requisitos de Software](docs/ERS%20TFG.pdf)
- [DAS - Documento de Arquitectura del Sistema](docs/DAS%20TFG.pdf)

## 👤 Autor

**[Tu nombre]**
- Universidad: [Nombre de la universidad]
- Tutor: [Nombre del tutor]

## 📄 Licencia

Este proyecto forma parte de un Trabajo de Fin de Grado y su uso está sujeto a las normativas académicas correspondientes.
