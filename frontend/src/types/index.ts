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
  SOLO_TEXTO = 'SOLO_TEXTO',
  OTRO = 'OTRO'
}

export enum EstadoEntrega {
  ENTREGADO = 'ENTREGADO',
  CALIFICADO = 'CALIFICADO',
  PUBLICADO = 'PUBLICADO'
}

export enum ModoOneDrive {
  ACTIVIDAD = 'ACTIVIDAD',
  ENTREGABLES = 'ENTREGABLES'
}

// DTOs
export interface UsuarioDTO {
  id: number;
  nombre: string;
  telefono?: string;
  correoElectronico: string;
  esAdmin: boolean;
  fotoPerfilUrl?: string;
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
  cursoIds?: number[];
  cursoTitulos?: string[];
  numeroEstudiantes: number;
}

export interface GuardarGrupoDTO {
  titulo: string;
  cursoIds: number[];
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
  subirAOneDrive?: boolean;
  oneDriveUsuarioId?: number;
  carpetaOneDrive?: string;
  modoOneDrive?: ModoOneDrive;
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
  subirAOneDrive?: boolean;
  oneDriveUsuarioId?: number;
  carpetaOneDrive?: string;
  modoOneDrive?: ModoOneDrive;
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
  estructuraZip?: string;
  validacionZipEstricta?: boolean;
  nombreZipEsperado?: string;
  actividadId: number;
  actividadTitulo: string;
  numeroEntregas: number;
  enPlazo: boolean;
  carpetaOneDrive?: string;
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
  estructuraZip?: string;
  validacionZipEstricta?: boolean;
  nombreZipEsperado?: string;
  carpetaOneDrive?: string;
}

// Estructura de nodo para el editor de estructura ZIP
export interface NodoEstructuraZip {
  id: string;
  nombre: string;       // nombre del archivo/carpeta, "*" = cualquiera
  tipo: 'ARCHIVO' | 'CARPETA';
  extensiones?: string[]; // para archivos: extensiones permitidas (vacio = cualquiera)
  hijos?: NodoEstructuraZip[]; // para carpetas: nodos hijos
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
  comentarioAlumno?: string;
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
  cursoId: number;
  cursoTitulo: string;
  actividadId: number;
  actividadTitulo: string;
  entregableId: number;
  entregableTitulo: string;
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
  nota: number;
  comentario?: string;
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

export interface EntregaPendienteDTO {
  entregableId: number;
  entregableTitulo: string;
  actividadId: number;
  actividadTitulo: string;
  cursoId: number;
  cursoTitulo: string;
  fechaLimite?: string | null;
  tiempoRestante: string;
}

export interface CambiarContrasenaDTO {
  contrasenaActual: string;
  contrasenaNueva: string;
}

export interface NotificacionDTO {
  id: number;
  tipo: 'NUEVO_ENTREGABLE' | 'DEADLINE_CERCANO';
  titulo: string;
  mensaje?: string;
  leida: boolean;
  cursoId?: number;
  fechaCreacion: string;
}

export interface PreferenciaNotificacionDTO {
  canal: 'APP' | 'EMAIL' | 'AMBOS';
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
  fotoPerfilUrl?: string;
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

export interface OneDriveFolder {
  id: string;
  name: string;
  path: string;
}

