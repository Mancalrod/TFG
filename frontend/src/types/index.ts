// Enums
export enum TipoActividad {
  EVALUABLE = 'EVALUABLE',
  NO_EVALUABLE = 'NO_EVALUABLE'
}

export enum Visibilidad {
  VISIBLE = 'VISIBLE',
  OCULTO = 'OCULTO'
}

export enum TipoMaterial {
  PDF = 'PDF',
  DOCX = 'DOCX',
  ZIP = 'ZIP',
  RAR = 'RAR',
  TXT = 'TXT',
  IMAGEN = 'IMAGEN',
  VIDEO = 'VIDEO',
  ENLACE = 'ENLACE',
  OTRO = 'OTRO'
}

export enum EstadoEntrega {
  ENTREGADO = 'ENTREGADO',
  CALIFICADO = 'CALIFICADO'
}

// DTOs
export interface UsuarioDTO {
  id: number;
  nombre: string;
  telefono?: string;
  correoElectronico: string;
  esAdmin: boolean;
}

export interface CrearUsuarioDTO {
  nombre: string;
  telefono?: string;
  correoElectronico: string;
  contrasena: string;
  esAdmin?: boolean;
}

export interface GrupoDTO {
  id: number;
  titulo: string;
  cursoId?: number;
  cursoTitulo?: string;
  numeroEstudiantes: number;
}

export interface CursoDTO {
  id: number;
  titulo: string;
  descripcion?: string;
  codigo: string;
  grupos: GrupoDTO[];
  numeroActividades: number;
  numeroProfesores: number;
  numeroEstudiantes: number;
}

export interface CrearCursoDTO {
  titulo: string;
  descripcion?: string;
  codigo: string;
}

export interface ActividadDTO {
  id: number;
  titulo: string;
  descripcion?: string;
  tipoActividad: TipoActividad;
  fechaCreacion: string;
  fechaInicio?: string;
  fechaLimite?: string;
  visibilidad: Visibilidad;
  notaMaxima?: number;
  cursoId: number;
  cursoTitulo: string;
  grupoIds: number[];
  numeroEntregables: number;
  enPlazo: boolean;
  entregables?: EntregableDTO[];
}

export interface CrearActividadDTO {
  titulo: string;
  descripcion?: string;
  tipoActividad: TipoActividad;
  fechaInicio?: string;
  fechaLimite?: string;
  visibilidad?: Visibilidad;
  notaMaxima?: number;
  grupoIds?: number[];
}

export interface EntregableDTO {
  id: number;
  titulo: string;
  descripcion?: string;
  fechaInicio?: string;
  fechaLimite?: string;
  notaMaxima?: number;
  tipoArchivoEsperado?: string;
  tamanoMaximoBytes?: number;
  visibilidad: Visibilidad;
  permiteReenvio: boolean;
  actividadId: number;
  actividadTitulo: string;
  numeroEntregas: number;
  enPlazo: boolean;
}

export interface CrearEntregableDTO {
  titulo: string;
  descripcion?: string;
  fechaInicio?: string;
  fechaLimite?: string;
  notaMaxima?: number;
  tipoArchivoEsperado?: string;
  tamanoMaximoBytes?: number;
  visibilidad?: Visibilidad;
  permiteReenvio?: boolean;
}

export interface MaterialDTO {
  id: number;
  nombre: string;
  tipoMaterial: TipoMaterial;
  ruta: string;
  tamanoBytes: number;
  onedriveFileId?: string;
  onedriveWebUrl?: string;
  almacenadoEnOneDrive: boolean;
}

export interface FeedbackDTO {
  id: number;
  comentario: string;
  fechaCreacion: string;
  fechaModificacion: string;
  entregaId: number;
  profesorId: number;
  profesorNombre: string;
}

export interface CrearFeedbackDTO {
  comentario: string;
}

export interface EntregaDTO {
  id: number;
  nombre: string;
  version: number;
  fechaEntrega: string;
  estado: EstadoEntrega;
  calificacion?: number;
  fechaCalificacion?: string;
  esVersionActiva: boolean;
  entregableId: number;
  entregableTitulo: string;
  estudianteId: number;
  estudianteNombre: string;
  fueATiempo: boolean;
  archivos: MaterialDTO[];
  feedbacks: FeedbackDTO[];
}

export interface EntregaResumenDTO {
  entregaId: number;
  estudianteId: number;
  estudianteNombre: string;
  estudianteCorreo: string;
  grupoTitulo: string;
  fechaEntrega: string;
  estado: EstadoEntrega;
  calificacion?: number;
  fueATiempo: boolean;
  version: number;
}

export interface CalificacionDTO {
  calificacion: number;
}

export interface EntregaEstadisticasDTO {
  entregableId: number;
  totalEntregas: number;
  entregasATiempo: number;
  entregasTardias: number;
  entregasCalificadas: number;
  entregasPendientes: number;
  promedioCalificacion?: number;
}

// Auth DTOs
export interface LoginRequestDTO {
  correoElectronico: string;
  contrasena: string;
}

export interface AuthResponseDTO {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  usuarioId: number;
  nombre: string;
  correoElectronico: string;
  roles: string[];
}

export interface RefreshTokenRequestDTO {
  refreshToken: string;
}

// Error response
export interface ErrorResponse {
  status: number;
  message: string;
  timestamp: string;
}

export interface ValidationErrorResponse extends ErrorResponse {
  errors: Record<string, string>;
}

// OneDrive DTOs
export interface OneDriveConnectionDTO {
  conectado: boolean;
  microsoftEmail?: string;
  fechaConexion?: string;
  fechaUltimoUso?: string;
  integrationEnabled: boolean;
}
