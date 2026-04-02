import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { usuarioService, cursoService } from '../../services';
import { UsuarioDTO, CrearUsuarioDTO, CursoDTO, CrearCursoDTO, GrupoDTO, GuardarGrupoDTO } from '../../types';
import './AdminPage.css';

type Tab = 'usuarios' | 'cursos' | 'grupos';

/* ════════════════════════════════════════════════════════
   ADMIN PAGE
   ════════════════════════════════════════════════════════ */
const AdminPage: React.FC = () => {
  const { esAdmin } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('usuarios');

  // ─── Global alert ───
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const showAlert = (type: 'success' | 'error', text: string) => {
    setAlert({ type, text });
    setTimeout(() => setAlert(null), 4000);
  };

  // Redirigir si no es admin
  useEffect(() => {
    if (!esAdmin) navigate('/dashboard', { replace: true });
  }, [esAdmin, navigate]);

  if (!esAdmin) return null;

  return (
    <div className="admin-page">
      <div className="admin-header">
        <h1>Panel de Administración</h1>
        <p className="admin-subtitle">Gestión de usuarios, cursos y grupos</p>
      </div>

      {alert && (
        <div className={`admin-alert admin-alert-${alert.type}`}>
          <span>{alert.text}</span>
          <button className="admin-alert-close" onClick={() => setAlert(null)}>×</button>
        </div>
      )}

      {/* Tabs */}
      <div className="admin-tabs">
        <button className={`admin-tab ${tab === 'usuarios' ? 'active' : ''}`} onClick={() => setTab('usuarios')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="18" height="18">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
          Usuarios
        </button>
        <button className={`admin-tab ${tab === 'cursos' ? 'active' : ''}`} onClick={() => setTab('cursos')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="18" height="18">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
          Cursos
        </button>
        <button className={`admin-tab ${tab === 'grupos' ? 'active' : ''}`} onClick={() => setTab('grupos')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="18" height="18">
            <rect x="3" y="3" width="7" height="7" />
            <rect x="14" y="3" width="7" height="7" />
            <rect x="3" y="14" width="7" height="7" />
            <rect x="14" y="14" width="7" height="7" />
          </svg>
          Grupos
        </button>
      </div>

      {/* Tab content */}
      <div className="admin-content">
        {tab === 'usuarios' && <UsuariosTab showAlert={showAlert} />}
        {tab === 'cursos' && <CursosTab showAlert={showAlert} />}
        {tab === 'grupos' && <GruposTab showAlert={showAlert} />}
      </div>
    </div>
  );
};

/* ════════════════════════════════════════════════════════
   USUARIOS TAB
   ════════════════════════════════════════════════════════ */
interface TabProps { showAlert: (t: 'success' | 'error', m: string) => void }

const UsuariosTab: React.FC<TabProps> = ({ showAlert }) => {
  const [usuarios, setUsuarios] = useState<UsuarioDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  // Modal create / edit
  const [modalOpen, setModalOpen] = useState(false);
  const [editando, setEditando] = useState<UsuarioDTO | null>(null);

  // Form
  const [form, setForm] = useState<CrearUsuarioDTO & { esAdmin?: boolean }>({
    nombre: '', correoElectronico: '', contrasena: '', telefono: '', esAdmin: false
  });

  // Roles modal
  const [rolesModal, setRolesModal] = useState<UsuarioDTO | null>(null);
  const [esProf, setEsProf] = useState(false);
  const [esEst, setEsEst] = useState(false);
  const [cursos, setCursos] = useState<CursoDTO[]>([]);
  const [grupos, setGrupos] = useState<GrupoDTO[]>([]);
  const [cursoSeleccionado, setCursoSeleccionado] = useState<number | ''>('');
  const [grupoSeleccionado, setGrupoSeleccionado] = useState<number | ''>('');

  const getApiErrorMessage = (err: unknown, fallback: string): string => {
    const axErr = err as { response?: { data?: { message?: string } } };
    return axErr?.response?.data?.message || fallback;
  };

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      setUsuarios(await usuarioService.listar());
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al cargar usuarios'));
    }
    finally { setLoading(false); }
  }, [showAlert]);

  useEffect(() => { cargar(); }, [cargar]);

  const filtrados = usuarios.filter(u => {
    const t = busqueda.toLowerCase();
    return u.nombre.toLowerCase().includes(t) || u.correoElectronico.toLowerCase().includes(t);
  });

  const openCrear = () => {
    setEditando(null);
    setForm({ nombre: '', correoElectronico: '', contrasena: '', telefono: '', esAdmin: false });
    setModalOpen(true);
  };

  const openEditar = (u: UsuarioDTO) => {
    setEditando(u);
    setForm({ nombre: u.nombre, correoElectronico: u.correoElectronico, contrasena: '', telefono: u.telefono || '', esAdmin: u.esAdmin });
    setModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editando) {
        await usuarioService.actualizar(editando.id, form);
        showAlert('success', 'Usuario actualizado');
      } else {
        await usuarioService.crear(form);
        showAlert('success', 'Usuario creado');
      }
      setModalOpen(false);
      cargar();
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al guardar usuario');
    }
  };

  const handleEliminar = async (id: number) => {
    if (!window.confirm('¿Eliminar este usuario?')) return;
    try {
      await usuarioService.eliminar(id);
      showAlert('success', 'Usuario eliminado');
      cargar();
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al eliminar usuario'));
    }
  };

  // ── Roles modal ──
  const openRolesModal = async (u: UsuarioDTO) => {
    setRolesModal(u);
    setCursoSeleccionado('');
    setGrupoSeleccionado('');
    try {
      const [prof, est, cursosData] = await Promise.all([
        usuarioService.esProfesor(u.id),
        usuarioService.esEstudiante(u.id),
        cursoService.listarTodos()
      ]);
      setEsProf(prof);
      setEsEst(est);
      setCursos(cursosData);
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al cargar roles'));
    }
  };

  const handleCursoChange = async (cursoId: number) => {
    setCursoSeleccionado(cursoId);
    setGrupoSeleccionado('');
    try {
      setGrupos(await cursoService.listarGrupos(cursoId));
    } catch (err: unknown) {
      setGrupos([]);
      showAlert('error', getApiErrorMessage(err, 'Error al cargar grupos del curso'));
    }
  };

  const handleAsignarProfesor = async () => {
    if (!rolesModal) return;
    try {
      await usuarioService.registrarComoProfesor(rolesModal.id);
      setEsProf(true);
      showAlert('success', `${rolesModal.nombre} ahora es profesor`);
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al asignar rol');
    }
  };

  const handleQuitarProfesor = async () => {
    if (!rolesModal) return;
    try {
      await usuarioService.eliminarRolProfesor(rolesModal.id);
      setEsProf(false);
      showAlert('success', 'Rol de profesor eliminado');
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al quitar rol'));
    }
  };

  const handleAsignarEstudiante = async () => {
    if (!rolesModal || grupoSeleccionado === '') return;
    try {
      await usuarioService.registrarComoEstudiante(rolesModal.id, grupoSeleccionado as number);
      setEsEst(true);
      showAlert('success', `${rolesModal.nombre} inscrito en el grupo`);
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al inscribir');
    }
  };

  const handleQuitarEstudiante = async () => {
    if (!rolesModal) return;
    try {
      await usuarioService.eliminarRolEstudiante(rolesModal.id);
      setEsEst(false);
      showAlert('success', 'Rol de estudiante eliminado');
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al quitar rol'));
    }
  };

  return (
    <div className="admin-tab-content">
      <div className="admin-toolbar">
        <div className="admin-search">
          <input
            type="text"
            placeholder="Buscar por nombre o correo..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
          />
        </div>
        <button className="admin-btn admin-btn-primary" onClick={openCrear}>
          + Nuevo Usuario
        </button>
      </div>

      {loading ? (
        <div className="admin-loading">Cargando usuarios...</div>
      ) : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Correo</th>
                <th>Teléfono</th>
                <th>Admin</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.length === 0 ? (
                <tr><td colSpan={6} className="admin-empty">No se encontraron usuarios</td></tr>
              ) : filtrados.map(u => (
                <tr key={u.id}>
                  <td className="admin-td-id">{u.id}</td>
                  <td>{u.nombre}</td>
                  <td className="admin-td-email">{u.correoElectronico}</td>
                  <td>{u.telefono || '—'}</td>
                  <td>
                    <span className={`admin-badge ${u.esAdmin ? 'badge-admin' : 'badge-default'}`}>
                      {u.esAdmin ? 'Sí' : 'No'}
                    </span>
                  </td>
                  <td className="admin-td-actions">
                    <button className="admin-btn-sm admin-btn-edit" onClick={() => openEditar(u)} title="Editar">
                      ✏️
                    </button>
                    <button className="admin-btn-sm admin-btn-roles" onClick={() => openRolesModal(u)} title="Roles">
                      🔑
                    </button>
                    <button className="admin-btn-sm admin-btn-delete" onClick={() => handleEliminar(u.id)} title="Eliminar">
                      🗑️
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal crear / editar */}
      {modalOpen && (
        <div className="admin-modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="admin-modal" onClick={e => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>{editando ? 'Editar Usuario' : 'Nuevo Usuario'}</h2>
              <button className="admin-modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <form onSubmit={handleSubmit} className="admin-form">
              <div className="admin-field">
                <label>Nombre *</label>
                <input type="text" required value={form.nombre}
                  onChange={e => setForm({ ...form, nombre: e.target.value })} />
              </div>
              <div className="admin-field">
                <label>Correo electrónico *</label>
                <input type="email" required value={form.correoElectronico}
                  onChange={e => setForm({ ...form, correoElectronico: e.target.value })} />
              </div>
              <div className="admin-field">
                <label>Contraseña {editando ? '(dejar vacío para no cambiar)' : '*'}</label>
                <input type="password" required={!editando} value={form.contrasena}
                  onChange={e => setForm({ ...form, contrasena: e.target.value })} />
              </div>
              <div className="admin-field">
                <label>Teléfono</label>
                <input type="text" value={form.telefono}
                  onChange={e => setForm({ ...form, telefono: e.target.value })} />
              </div>
              <div className="admin-field-check">
                <label>
                  <input type="checkbox" checked={form.esAdmin || false}
                    onChange={e => setForm({ ...form, esAdmin: e.target.checked })} />
                  Administrador
                </label>
              </div>
              <div className="admin-form-actions">
                <button type="button" className="admin-btn admin-btn-secondary" onClick={() => setModalOpen(false)}>
                  Cancelar
                </button>
                <button type="submit" className="admin-btn admin-btn-primary">
                  {editando ? 'Guardar' : 'Crear'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal roles */}
      {rolesModal && (
        <div className="admin-modal-overlay" onClick={() => setRolesModal(null)}>
          <div className="admin-modal admin-modal-roles" onClick={e => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>Roles — {rolesModal.nombre}</h2>
              <button className="admin-modal-close" onClick={() => setRolesModal(null)}>×</button>
            </div>
            <div className="admin-roles-content">
              {/* Profesor */}
              <div className="admin-role-section">
                <div className="admin-role-header">
                  <h3>Profesor</h3>
                  <span className={`admin-badge ${esProf ? 'badge-success' : 'badge-default'}`}>
                    {esProf ? 'Activo' : 'No asignado'}
                  </span>
                </div>
                {esProf ? (
                  <button className="admin-btn admin-btn-danger" onClick={handleQuitarProfesor}>
                    Quitar rol de profesor
                  </button>
                ) : (
                  <button className="admin-btn admin-btn-primary" onClick={handleAsignarProfesor}>
                    Asignar como profesor
                  </button>
                )}
              </div>

              {/* Estudiante */}
              <div className="admin-role-section">
                <div className="admin-role-header">
                  <h3>Estudiante</h3>
                  <span className={`admin-badge ${esEst ? 'badge-success' : 'badge-default'}`}>
                    {esEst ? 'Activo' : 'No asignado'}
                  </span>
                </div>
                {esEst ? (
                  <button className="admin-btn admin-btn-danger" onClick={handleQuitarEstudiante}>
                    Quitar rol de estudiante
                  </button>
                ) : (
                  <div className="admin-role-enroll">
                    <select value={cursoSeleccionado} onChange={e => handleCursoChange(Number(e.target.value))}>
                      <option value="">Selecciona un curso...</option>
                      {cursos.map(c => <option key={c.id} value={c.id}>{c.titulo} ({c.codigo})</option>)}
                    </select>
                    {grupos.length > 0 && (
                      <select value={grupoSeleccionado} onChange={e => setGrupoSeleccionado(Number(e.target.value))}>
                        <option value="">Selecciona un grupo...</option>
                        {grupos.map(g => <option key={g.id} value={g.id}>{g.titulo}</option>)}
                      </select>
                    )}
                    <button
                      className="admin-btn admin-btn-primary"
                      disabled={grupoSeleccionado === ''}
                      onClick={handleAsignarEstudiante}
                    >
                      Inscribir en grupo
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

/* ════════════════════════════════════════════════════════
   CURSOS TAB
   ════════════════════════════════════════════════════════ */
const CursosTab: React.FC<TabProps> = ({ showAlert }) => {
  const [cursos, setCursos] = useState<CursoDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editando, setEditando] = useState<CursoDTO | null>(null);
  const [form, setForm] = useState<CrearCursoDTO>({ titulo: '', descripcion: '', codigo: '' });

  // Profesor assignment
  const [profModal, setProfModal] = useState<CursoDTO | null>(null);
  const [usuarios, setUsuarios] = useState<UsuarioDTO[]>([]);
  const [profesSeleccionados, setProfesSeleccionados] = useState<number[]>([]);
  const [profBusquedaCrear, setProfBusquedaCrear] = useState('');
  const [profBusquedaEdicion, setProfBusquedaEdicion] = useState('');
  const [profesSeleccionadosEdicion, setProfesSeleccionadosEdicion] = useState<number[]>([]);
  const [profesOriginalesEdicion, setProfesOriginalesEdicion] = useState<number[]>([]);
  const [cargandoProfesCurso, setCargandoProfesCurso] = useState(false);
  const [guardandoProfesCurso, setGuardandoProfesCurso] = useState(false);

  const getApiErrorMessage = (err: unknown, fallback: string): string => {
    const axErr = err as { response?: { data?: { message?: string } } };
    return axErr?.response?.data?.message || fallback;
  };

  const cargar = useCallback(async () => {
    setLoading(true);
    try { setCursos(await cursoService.listarTodos()); }
    catch (err: unknown) { showAlert('error', getApiErrorMessage(err, 'Error al cargar cursos')); }
    finally { setLoading(false); }
  }, [showAlert]);

  useEffect(() => { cargar(); }, [cargar]);

  const filtrados = cursos.filter(c => {
    const t = busqueda.toLowerCase();
    return c.titulo.toLowerCase().includes(t) || c.codigo.toLowerCase().includes(t);
  });

  const openEditar = (c: CursoDTO) => {
    setEditando(c);
    setForm({ titulo: c.titulo, descripcion: c.descripcion || '', codigo: c.codigo });
    setCrearConProf(false);
    setModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editando) {
        await cursoService.actualizar(editando.id, form);
        showAlert('success', 'Curso actualizado');
      }
      setModalOpen(false);
      cargar();
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al guardar curso');
    }
  };

  // Create curso with professor selection
  const [crearConProf, setCrearConProf] = useState(false);
  const handleCrearCurso = async (e: React.FormEvent) => {
    e.preventDefault();
    if (profesSeleccionados.length === 0) {
      showAlert('error', 'Selecciona al menos un profesor para el curso');
      return;
    }
    try {
      const usuarioIds = [...new Set(profesSeleccionados)];
      const [primerUsuarioId, ...restoUsuarios] = usuarioIds;

      // El primer usuario crea el curso y queda asignado como profesor del curso.
      const cursoCreado = await cursoService.crearPorUsuario(primerUsuarioId, form);

      // Asignar el resto de seleccionados como profesores del curso.
      for (const usuarioId of restoUsuarios) {
        await cursoService.agregarProfesorPorUsuario(cursoCreado.id, usuarioId);
      }

      showAlert('success', 'Curso creado');
      setModalOpen(false);
      setCrearConProf(false);
      setProfesSeleccionados([]);
      cargar();
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al crear curso');
    }
  };

  const openCrearConProf = async () => {
    setEditando(null);
    setForm({ titulo: '', descripcion: '', codigo: '' });
    setProfesSeleccionados([]);
    setProfBusquedaCrear('');
    try {
      setUsuarios(await usuarioService.listar());
    } catch {
      // Best effort: si falla esta carga, el modal igualmente se abre.
    }
    setCrearConProf(true);
    setModalOpen(true);
  };

  const handleEliminar = async (id: number) => {
    if (!window.confirm('¿Eliminar este curso y todos sus datos?')) return;
    try {
      await cursoService.eliminar(id);
      showAlert('success', 'Curso eliminado');
      cargar();
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al eliminar curso'));
    }
  };

  // Professor assignment modal
  const openProfModal = async (c: CursoDTO) => {
    setProfModal(c);
    setProfBusquedaEdicion('');
    setCargandoProfesCurso(true);
    try {
      const users = await usuarioService.listar();
      setUsuarios(users);

      const candidatos = users.filter(u => !u.esAdmin);
      const idsProfesCurso = (await Promise.all(
        candidatos.map(async (u) => {
          const isProf = await usuarioService.esProfesor(u.id);
          if (!isProf) return null;
          const cursosProfesor = await cursoService.listarPorProfesor(u.id);
          return cursosProfesor.some(cp => cp.id === c.id) ? u.id : null;
        })
      )).filter((id): id is number => id !== null);

      setProfesSeleccionadosEdicion(idsProfesCurso);
      setProfesOriginalesEdicion(idsProfesCurso);
    } catch {
      // Best effort: si falla esta carga, el usuario puede cerrar y reintentar.
    } finally {
      setCargandoProfesCurso(false);
    }
  };

  const toggleProfesorEdicion = (usuarioId: number) => {
    setProfesSeleccionadosEdicion(prev =>
      prev.includes(usuarioId)
        ? prev.filter(id => id !== usuarioId)
        : [...prev, usuarioId]
    );
  };

  const revertirSeleccionProfesores = () => {
    setProfesSeleccionadosEdicion(profesOriginalesEdicion);
  };

  const guardarCambiosProfesores = async () => {
    if (!profModal) return;
    setGuardandoProfesCurso(true);
    try {
      const originalSet = new Set(profesOriginalesEdicion);
      const selectedSet = new Set(profesSeleccionadosEdicion);

      const aAgregar = profesSeleccionadosEdicion.filter(id => !originalSet.has(id));
      const aQuitar = profesOriginalesEdicion.filter(id => !selectedSet.has(id));

      for (const usuarioId of aAgregar) {
        await cursoService.agregarProfesorPorUsuario(profModal.id, usuarioId);
      }

      for (const usuarioId of aQuitar) {
        await cursoService.quitarProfesorPorUsuario(profModal.id, usuarioId);
      }

      setProfesOriginalesEdicion(profesSeleccionadosEdicion);
      setProfModal({ ...profModal, numeroProfesores: profesSeleccionadosEdicion.length });
      showAlert('success', 'Profesores del curso actualizados');
      setProfModal(null);
      cargar();
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al guardar profesores del curso'));
    } finally {
      setGuardandoProfesCurso(false);
    }
  };

  const toggleProfesorSeleccionado = (usuarioId: number) => {
    setProfesSeleccionados(prev =>
      prev.includes(usuarioId)
        ? prev.filter(id => id !== usuarioId)
        : [...prev, usuarioId]
    );
  };

  const profesoresDisponiblesCrear = usuarios.filter(u => !u.esAdmin);
  const profesoresFiltradosCrear = profesoresDisponiblesCrear.filter(u => {
    const t = profBusquedaCrear.trim().toLowerCase();
    if (!t) return true;
    return u.nombre.toLowerCase().includes(t) || u.correoElectronico.toLowerCase().includes(t);
  });

  const seleccionarTodosFiltrados = () => {
    const idsFiltrados = profesoresFiltradosCrear.map(u => u.id);
    setProfesSeleccionados(prev => [...new Set([...prev, ...idsFiltrados])]);
  };

  const limpiarSeleccionProfes = () => {
    setProfesSeleccionados([]);
  };

  const profesoresFiltradosEdicion = usuarios
    .filter(u => !u.esAdmin)
    .filter(u => {
      const t = profBusquedaEdicion.trim().toLowerCase();
      if (!t) return true;
      return u.nombre.toLowerCase().includes(t) || u.correoElectronico.toLowerCase().includes(t);
    });

  return (
    <div className="admin-tab-content">
      <div className="admin-toolbar">
        <div className="admin-search">
          <input type="text" placeholder="Buscar por título o código..."
            value={busqueda} onChange={e => setBusqueda(e.target.value)} />
        </div>
        <button className="admin-btn admin-btn-primary" onClick={openCrearConProf}>
          + Nuevo Curso
        </button>
      </div>

      {loading ? (
        <div className="admin-loading">Cargando cursos...</div>
      ) : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Código</th>
                <th>Título</th>
                <th>Profesores</th>
                <th>Estudiantes</th>
                <th>Grupos</th>
                <th>Actividades</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.length === 0 ? (
                <tr><td colSpan={8} className="admin-empty">No se encontraron cursos</td></tr>
              ) : filtrados.map(c => (
                <tr key={c.id}>
                  <td className="admin-td-id">{c.id}</td>
                  <td><span className="admin-badge badge-code">{c.codigo}</span></td>
                  <td>{c.titulo}</td>
                  <td>{c.numeroProfesores}</td>
                  <td>{c.numeroEstudiantes}</td>
                  <td>{c.grupos.length}</td>
                  <td>{c.numeroActividades}</td>
                  <td className="admin-td-actions">
                    <button className="admin-btn-sm admin-btn-edit" onClick={() => openEditar(c)} title="Editar">
                      ✏️
                    </button>
                    <button className="admin-btn-sm admin-btn-roles" onClick={() => openProfModal(c)} title="Profesores">
                      👨‍🏫
                    </button>
                    <button className="admin-btn-sm admin-btn-delete" onClick={() => handleEliminar(c.id)} title="Eliminar">
                      🗑️
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal crear/editar curso */}
      {modalOpen && (
        <div className="admin-modal-overlay" onClick={() => { setModalOpen(false); setCrearConProf(false); }}>
          <div className="admin-modal" onClick={e => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>{editando ? 'Editar Curso' : 'Nuevo Curso'}</h2>
              <button className="admin-modal-close" onClick={() => { setModalOpen(false); setCrearConProf(false); }}>×</button>
            </div>
            <form onSubmit={editando ? handleSubmit : handleCrearCurso} className="admin-form">
              <div className="admin-field">
                <label>Título *</label>
                <input className="admin-input-emphasis" type="text" required value={form.titulo}
                  onChange={e => setForm({ ...form, titulo: e.target.value })} />
              </div>
              <div className="admin-field">
                <label>Código *</label>
                <input className="admin-input-emphasis" type="text" required value={form.codigo}
                  onChange={e => setForm({ ...form, codigo: e.target.value })} />
              </div>
              <div className="admin-field">
                <label>Descripción</label>
                <textarea className="admin-input-emphasis" value={form.descripcion || ''}
                  onChange={e => setForm({ ...form, descripcion: e.target.value })} rows={3} />
              </div>
              {crearConProf && !editando && (
                <div className="admin-field">
                  <label>Profesores del curso *</label>
                  <div className="admin-checklist-toolbar">
                    <input
                      type="text"
                      className="admin-checklist-search"
                      value={profBusquedaCrear}
                      onChange={e => setProfBusquedaCrear(e.target.value)}
                      placeholder="Buscar por nombre o correo..."
                    />
                    <div className="admin-checklist-actions">
                      <button
                        type="button"
                        className="admin-btn admin-btn-secondary"
                        onClick={seleccionarTodosFiltrados}
                        disabled={profesoresFiltradosCrear.length === 0}
                      >
                        Seleccionar visibles
                      </button>
                      <button
                        type="button"
                        className="admin-btn admin-btn-secondary"
                        onClick={limpiarSeleccionProfes}
                        disabled={profesSeleccionados.length === 0}
                      >
                        Limpiar
                      </button>
                    </div>
                  </div>

                  <div className="admin-checklist-summary">
                    <span className="admin-badge badge-success">
                      {profesSeleccionados.length} seleccionado(s)
                    </span>
                  </div>

                  <div className="admin-checklist">
                    {profesoresDisponiblesCrear.length === 0 ? (
                      <p className="admin-checklist-empty">No hay usuarios disponibles para asignar.</p>
                    ) : profesoresFiltradosCrear.length === 0 ? (
                      <p className="admin-checklist-empty">No hay coincidencias para la búsqueda actual.</p>
                    ) : (
                      profesoresFiltradosCrear.map(u => (
                          <label key={u.id} className="admin-checklist-item">
                            <span className="admin-checklist-avatar" aria-hidden="true">
                              {u.nombre.charAt(0).toUpperCase()}
                            </span>
                            <span className="admin-checklist-text">
                              <strong>{u.nombre}</strong>
                              <small>{u.correoElectronico}</small>
                            </span>
                            <span className="admin-checklist-check">
                              <input
                                className="admin-checklist-checkbox"
                                type="checkbox"
                                checked={profesSeleccionados.includes(u.id)}
                                onChange={() => toggleProfesorSeleccionado(u.id)}
                              />
                            </span>
                          </label>
                      ))
                    )}
                  </div>
                  <small className="admin-checklist-help">
                    Los usuarios seleccionados quedarán asignados como profesores del curso.
                  </small>
                </div>
              )}
              <div className="admin-form-actions">
                <button type="button" className="admin-btn admin-btn-secondary"
                  onClick={() => { setModalOpen(false); setCrearConProf(false); }}>Cancelar</button>
                <button type="submit" className="admin-btn admin-btn-primary">
                  {editando ? 'Guardar' : 'Crear'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal asignar profesores */}
      {profModal && (
        <div className="admin-modal-overlay" onClick={() => setProfModal(null)}>
          <div className="admin-modal" onClick={e => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>Profesores — {profModal.titulo}</h2>
              <button className="admin-modal-close" onClick={() => setProfModal(null)}>×</button>
            </div>
            <div className="admin-roles-content">
              <p className="admin-info-text">
                El curso tiene <strong>{profModal.numeroProfesores}</strong> profesor(es) asignado(s).
              </p>
              {cargandoProfesCurso ? (
                <div className="admin-loading">Cargando profesores del curso...</div>
              ) : (
                <>
                  <div className="admin-checklist-toolbar">
                    <input
                      type="text"
                      className="admin-checklist-search"
                      value={profBusquedaEdicion}
                      onChange={e => setProfBusquedaEdicion(e.target.value)}
                      placeholder="Buscar por nombre o correo..."
                    />
                    <div className="admin-checklist-actions">
                      <button
                        type="button"
                        className="admin-btn admin-btn-secondary"
                        onClick={revertirSeleccionProfesores}
                        disabled={guardandoProfesCurso}
                      >
                        Revertir
                      </button>
                    </div>
                  </div>

                  <div className="admin-checklist-summary">
                    <span className="admin-badge badge-success">
                      {profesSeleccionadosEdicion.length} seleccionado(s)
                    </span>
                  </div>

                  <div className="admin-checklist">
                    {profesoresFiltradosEdicion.length === 0 ? (
                      <p className="admin-checklist-empty">No hay coincidencias para la búsqueda actual.</p>
                    ) : (
                      profesoresFiltradosEdicion.map(u => (
                        <label key={u.id} className="admin-checklist-item">
                          <span className="admin-checklist-avatar" aria-hidden="true">
                            {u.nombre.charAt(0).toUpperCase()}
                          </span>
                          <span className="admin-checklist-text">
                            <strong>{u.nombre}</strong>
                            <small>{u.correoElectronico}</small>
                          </span>
                          <span className="admin-checklist-check">
                            <input
                              className="admin-checklist-checkbox"
                              type="checkbox"
                              checked={profesSeleccionadosEdicion.includes(u.id)}
                              onChange={() => toggleProfesorEdicion(u.id)}
                            />
                          </span>
                        </label>
                      ))
                    )}
                  </div>

                  <div className="admin-form-actions">
                    <button type="button" className="admin-btn admin-btn-secondary" onClick={() => setProfModal(null)}>
                      Cancelar
                    </button>
                    <button
                      type="button"
                      className="admin-btn admin-btn-primary"
                      onClick={guardarCambiosProfesores}
                      disabled={guardandoProfesCurso}
                    >
                      {guardandoProfesCurso ? 'Guardando...' : 'Guardar cambios'}
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

/* ════════════════════════════════════════════════════════
   GRUPOS TAB
   ════════════════════════════════════════════════════════ */
const GruposTab: React.FC<TabProps> = ({ showAlert }) => {
  const [cursos, setCursos] = useState<CursoDTO[]>([]);
  const [grupos, setGrupos] = useState<GrupoDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');

  // Create / edit grupo
  const [modalOpen, setModalOpen] = useState(false);
  const [editandoGrupo, setEditandoGrupo] = useState<GrupoDTO | null>(null);
  const [tituloGrupo, setTituloGrupo] = useState('');
  const [cursosSeleccionadosGrupo, setCursosSeleccionadosGrupo] = useState<number[]>([]);
  const [busquedaCursosGrupo, setBusquedaCursosGrupo] = useState('');
  const [grupoModalAlert, setGrupoModalAlert] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Estudiantes de un grupo
  const [estModal, setEstModal] = useState<GrupoDTO | null>(null);
  const [todosUsuarios, setTodosUsuarios] = useState<UsuarioDTO[]>([]);
  const [estBusqueda, setEstBusqueda] = useState('');
  const [estSeleccionados, setEstSeleccionados] = useState<number[]>([]);
  const [estOriginales, setEstOriginales] = useState<number[]>([]);
  const [guardandoEstudiantes, setGuardandoEstudiantes] = useState(false);
  const [estModalAlert, setEstModalAlert] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const getApiErrorMessage = (err: unknown, fallback: string): string => {
    const axErr = err as { response?: { data?: { message?: string } } };
    return axErr?.response?.data?.message || fallback;
  };

  const cargarTodo = useCallback(async () => {
    setLoading(true);
    try {
      const [cursosData, gruposData] = await Promise.all([
        cursoService.listarTodos(),
        cursoService.listarTodosGrupos()
      ]);
      setCursos(cursosData);
      setGrupos(gruposData);
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al cargar grupos'));
    }
    finally { setLoading(false); }
  }, [showAlert]);

  useEffect(() => { cargarTodo(); }, [cargarTodo]);

  const filtrados = grupos.filter(g => {
    const t = busqueda.trim().toLowerCase();
    if (!t) return true;
    const cursosTexto = (g.cursoTitulos || (g.cursoTitulo ? [g.cursoTitulo] : [])).join(' ').toLowerCase();
    return g.titulo.toLowerCase().includes(t) || cursosTexto.includes(t);
  });

  const cursosFiltradosGrupo = cursos.filter(c => {
    const t = busquedaCursosGrupo.trim().toLowerCase();
    if (!t) return true;
    return c.titulo.toLowerCase().includes(t) || c.codigo.toLowerCase().includes(t);
  });

  const toggleCursoGrupo = (cursoId: number) => {
    setCursosSeleccionadosGrupo(prev =>
      prev.includes(cursoId) ? prev.filter(id => id !== cursoId) : [...prev, cursoId]
    );
  };

  const openCrearGrupo = () => {
    setEditandoGrupo(null);
    setTituloGrupo('');
    setCursosSeleccionadosGrupo([]);
    setBusquedaCursosGrupo('');
    setGrupoModalAlert(null);
    setModalOpen(true);
  };

  const openEditarGrupo = (g: GrupoDTO) => {
    setEditandoGrupo(g);
    setTituloGrupo(g.titulo);
    const ids = g.cursoIds && g.cursoIds.length > 0
      ? g.cursoIds
      : (g.cursoId ? [g.cursoId] : []);
    setCursosSeleccionadosGrupo(ids);
    setBusquedaCursosGrupo('');
    setGrupoModalAlert(null);
    setModalOpen(true);
  };

  const handleSubmitGrupo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cursosSeleccionadosGrupo.length === 0) {
      setGrupoModalAlert({ type: 'error', text: 'Selecciona al menos un curso para el grupo' });
      return;
    }

    const payload: GuardarGrupoDTO = {
      titulo: tituloGrupo,
      cursoIds: cursosSeleccionadosGrupo
    };

    try {
      if (editandoGrupo) {
        await cursoService.actualizarGrupoConCursos(editandoGrupo.id, payload);
        showAlert('success', 'Grupo actualizado');
      } else {
        await cursoService.crearGrupoConCursos(payload);
        showAlert('success', 'Grupo creado');
      }
      setModalOpen(false);
      setGrupoModalAlert(null);
      cargarTodo();
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      setGrupoModalAlert({ type: 'error', text: axErr?.response?.data?.message || 'Error al guardar grupo' });
    }
  };

  const handleEliminarGrupo = async (gid: number) => {
    if (!window.confirm('¿Eliminar este grupo?')) return;
    try {
      await cursoService.eliminarGrupo(gid);
      showAlert('success', 'Grupo eliminado');
      cargarTodo();
    } catch (err: unknown) {
      showAlert('error', getApiErrorMessage(err, 'Error al eliminar grupo'));
    }
  };

  // Estudiantes modal
  const openEstModal = async (g: GrupoDTO) => {
    setEstModal(g);
    setEstBusqueda('');
    setEstModalAlert(null);
    try {
      const [ests, users] = await Promise.all([
        usuarioService.listarEstudiantesDeGrupo(g.id),
        usuarioService.listar()
      ]);
      setTodosUsuarios(users);
      const ids = ests.map(e => e.id);
      setEstOriginales(ids);
      setEstSeleccionados(ids);
    } catch (err: unknown) {
      setEstModalAlert({ type: 'error', text: getApiErrorMessage(err, 'Error al cargar estudiantes') });
    }
  };

  const toggleEstudianteSeleccionado = (usuarioId: number) => {
    setEstSeleccionados(prev =>
      prev.includes(usuarioId) ? prev.filter(id => id !== usuarioId) : [...prev, usuarioId]
    );
  };

  const revertirSeleccionEstudiantes = () => {
    setEstSeleccionados(estOriginales);
  };

  const guardarCambiosEstudiantes = async () => {
    if (!estModal) return;
    setGuardandoEstudiantes(true);
    try {
      const original = new Set(estOriginales);
      const actual = new Set(estSeleccionados);
      const aAgregar = estSeleccionados.filter(id => !original.has(id));
      const aQuitar = estOriginales.filter(id => !actual.has(id));

      for (const userId of aAgregar) {
        await usuarioService.registrarComoEstudiante(userId, estModal.id);
      }
      for (const userId of aQuitar) {
        await usuarioService.eliminarEstudianteDeGrupo(userId, estModal.id);
      }

      const recargados = await usuarioService.listarEstudiantesDeGrupo(estModal.id);
  setEstOriginales(recargados.map(e => e.id));
  setEstSeleccionados(recargados.map(e => e.id));
      showAlert('success', 'Estudiantes actualizados');
      setEstModalAlert(null);
      setEstModal(null);
      cargarTodo();
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      setEstModalAlert({ type: 'error', text: axErr?.response?.data?.message || 'Error al actualizar estudiantes' });
    } finally {
      setGuardandoEstudiantes(false);
    }
  };

  const extraerCorreos = (texto: string): string[] => {
    const regex = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi;
    const matches = texto.match(regex) || [];
    return [...new Set(matches.map(m => m.trim().toLowerCase()))];
  };

  const handleImportarCorreos = async (file: File | null) => {
    if (!file) return;
    try {
      let correos: string[] = [];
      const nombre = file.name.toLowerCase();

      if (nombre.endsWith('.txt') || nombre.endsWith('.csv')) {
        const contenido = await file.text();
        correos = extraerCorreos(contenido);
      } else if (nombre.endsWith('.xlsx') || nombre.endsWith('.xls')) {
        const xlsx = await import('xlsx');
        const data = await file.arrayBuffer();
        const wb = xlsx.read(data, { type: 'array' });
        const celdas: string[] = [];
        wb.SheetNames.forEach(sheetName => {
          const sheet = wb.Sheets[sheetName];
          const rows = xlsx.utils.sheet_to_json<(string | number | null)[]>(sheet, { header: 1 });
          rows.forEach(row => row.forEach(cell => celdas.push(String(cell ?? ''))));
        });
        correos = extraerCorreos(celdas.join('\n'));
      } else {
        setEstModalAlert({ type: 'error', text: 'Formato no soportado. Usa TXT, CSV, XLS o XLSX' });
        return;
      }

      if (correos.length === 0) {
        setEstModalAlert({ type: 'error', text: 'No se encontraron correos válidos en el archivo' });
        return;
      }

      const byEmail = new Map(
        todosUsuarios
          .filter(u => !u.esAdmin)
          .map(u => [u.correoElectronico.toLowerCase(), u.id] as const)
      );

      const idsEncontrados = correos
        .map(c => byEmail.get(c))
        .filter((id): id is number => id !== undefined);

      const noEncontrados = correos.filter(c => !byEmail.has(c));

      if (idsEncontrados.length > 0) {
        setEstSeleccionados(prev => [...new Set([...prev, ...idsEncontrados])]);
      }

      const resumen = `Importados ${idsEncontrados.length} alumno(s). ${noEncontrados.length} correo(s) sin coincidencia.`;
      if (idsEncontrados.length > 0) {
        setEstModalAlert({ type: 'success', text: resumen });
      } else {
        setEstModalAlert({ type: 'error', text: resumen });
      }
    } catch {
      setEstModalAlert({ type: 'error', text: 'No se pudo procesar el archivo de correos' });
    }
  };

  const estudiantesFiltrados = todosUsuarios
    .filter(u => !u.esAdmin)
    .filter(u => {
      const t = estBusqueda.trim().toLowerCase();
      if (!t) return true;
      return u.nombre.toLowerCase().includes(t) || u.correoElectronico.toLowerCase().includes(t);
    });

  return (
    <div className="admin-tab-content">
      <div className="admin-toolbar">
        <div className="admin-search">
          <input
            type="text"
            placeholder="Buscar grupo o curso asociado..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
          />
        </div>
        <button className="admin-btn admin-btn-primary" onClick={openCrearGrupo}>
          + Nuevo Grupo
        </button>
      </div>

      {loading ? (
        <div className="admin-loading">Cargando grupos...</div>
      ) : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Título</th>
                <th>Cursos</th>
                <th>Estudiantes</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.length === 0 ? (
                <tr><td colSpan={5} className="admin-empty">No hay grupos para los filtros actuales</td></tr>
              ) : filtrados.map(g => (
                <tr key={g.id}>
                  <td className="admin-td-id">{g.id}</td>
                  <td>{g.titulo}</td>
                  <td>{(g.cursoTitulos || (g.cursoTitulo ? [g.cursoTitulo] : [])).join(', ')}</td>
                  <td>{g.numeroEstudiantes}</td>
                  <td className="admin-td-actions">
                    <button className="admin-btn-sm admin-btn-edit" onClick={() => openEditarGrupo(g)} title="Editar">
                      ✏️
                    </button>
                    <button className="admin-btn-sm admin-btn-roles" onClick={() => openEstModal(g)} title="Estudiantes">
                      👥
                    </button>
                    <button className="admin-btn-sm admin-btn-delete" onClick={() => handleEliminarGrupo(g.id)} title="Eliminar">
                      🗑️
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal crear/editar grupo */}
      {modalOpen && (
        <div className="admin-modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="admin-modal admin-modal-sm" onClick={e => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>{editandoGrupo ? 'Editar Grupo' : 'Nuevo Grupo'}</h2>
              <button className="admin-modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <form onSubmit={handleSubmitGrupo} className="admin-form">
              {grupoModalAlert && (
                <div className={`admin-inline-alert admin-inline-alert-${grupoModalAlert.type}`}>
                  {grupoModalAlert.text}
                </div>
              )}
              <div className="admin-field">
                <label>Título del grupo *</label>
                <input type="text" required value={tituloGrupo}
                  onChange={e => setTituloGrupo(e.target.value)} placeholder="Ej: Grupo A" />
              </div>
              <div className="admin-field">
                <label>Cursos asociados *</label>
                <div className="admin-checklist-toolbar">
                  <input
                    type="text"
                    className="admin-checklist-search"
                    value={busquedaCursosGrupo}
                    onChange={e => setBusquedaCursosGrupo(e.target.value)}
                    placeholder="Buscar curso por título o código..."
                  />
                </div>
                <div className="admin-checklist-summary">
                  <span className="admin-badge badge-success">{cursosSeleccionadosGrupo.length} seleccionado(s)</span>
                </div>
                <div className="admin-checklist">
                  {cursosFiltradosGrupo.length === 0 ? (
                    <p className="admin-checklist-empty">No hay cursos para la búsqueda actual.</p>
                  ) : (
                    cursosFiltradosGrupo.map(c => (
                      <label key={c.id} className="admin-checklist-item">
                        <span className="admin-checklist-avatar" aria-hidden="true">{c.titulo.charAt(0).toUpperCase()}</span>
                        <span className="admin-checklist-text">
                          <strong>{c.titulo}</strong>
                          <small>{c.codigo}</small>
                        </span>
                        <span className="admin-checklist-check">
                          <input
                            className="admin-checklist-checkbox"
                            type="checkbox"
                            checked={cursosSeleccionadosGrupo.includes(c.id)}
                            onChange={() => toggleCursoGrupo(c.id)}
                          />
                        </span>
                      </label>
                    ))
                  )}
                </div>
              </div>
              <div className="admin-form-actions">
                <button type="button" className="admin-btn admin-btn-secondary" onClick={() => setModalOpen(false)}>
                  Cancelar
                </button>
                <button type="submit" className="admin-btn admin-btn-primary">
                  {editandoGrupo ? 'Guardar' : 'Crear'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal estudiantes */}
      {estModal && (
        <div className="admin-modal-overlay" onClick={() => setEstModal(null)}>
          <div className="admin-modal admin-modal-wide" onClick={e => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>Estudiantes — {estModal.titulo}</h2>
              <button className="admin-modal-close" onClick={() => setEstModal(null)}>×</button>
            </div>
            <div className="admin-roles-content">
              {estModalAlert && (
                <div className={`admin-inline-alert admin-inline-alert-${estModalAlert.type}`}>
                  {estModalAlert.text}
                </div>
              )}
              <div className="admin-checklist-toolbar">
                <input
                  type="text"
                  className="admin-checklist-search"
                  placeholder="Buscar por nombre o correo..."
                  value={estBusqueda}
                  onChange={e => setEstBusqueda(e.target.value)}
                />
                <div className="admin-checklist-actions">
                  <label className="admin-btn admin-btn-secondary" htmlFor="estudiantes-import-file">
                    Importar TXT/Excel
                  </label>
                  <input
                    id="estudiantes-import-file"
                    type="file"
                    accept=".txt,.csv,.xls,.xlsx"
                    style={{ display: 'none' }}
                    onChange={e => {
                      const file = e.target.files?.[0] || null;
                      handleImportarCorreos(file);
                      e.currentTarget.value = '';
                    }}
                  />
                  <button
                    type="button"
                    className="admin-btn admin-btn-secondary"
                    onClick={revertirSeleccionEstudiantes}
                    disabled={guardandoEstudiantes}
                  >
                    Revertir
                  </button>
                </div>
              </div>

              <div className="admin-checklist-summary">
                <span className="admin-badge badge-success">{estSeleccionados.length} seleccionado(s)</span>
              </div>

              <div className="admin-checklist">
                {estudiantesFiltrados.length === 0 ? (
                  <p className="admin-checklist-empty">No hay usuarios para la búsqueda actual.</p>
                ) : (
                  estudiantesFiltrados.map(u => (
                    <label key={u.id} className="admin-checklist-item">
                      <span className="admin-checklist-avatar" aria-hidden="true">{u.nombre.charAt(0).toUpperCase()}</span>
                      <span className="admin-checklist-text">
                        <strong>{u.nombre}</strong>
                        <small>{u.correoElectronico}</small>
                      </span>
                      <span className="admin-checklist-check">
                        <input
                          className="admin-checklist-checkbox"
                          type="checkbox"
                          checked={estSeleccionados.includes(u.id)}
                          onChange={() => toggleEstudianteSeleccionado(u.id)}
                        />
                      </span>
                    </label>
                  ))
                )}
              </div>

              <div className="admin-form-actions">
                <button type="button" className="admin-btn admin-btn-secondary" onClick={() => setEstModal(null)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="admin-btn admin-btn-primary"
                  onClick={guardarCambiosEstudiantes}
                  disabled={guardandoEstudiantes}
                >
                  {guardandoEstudiantes ? 'Guardando...' : 'Guardar cambios'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPage;
