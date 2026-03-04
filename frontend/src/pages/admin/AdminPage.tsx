import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { usuarioService, cursoService } from '../../services';
import { UsuarioDTO, CrearUsuarioDTO, CursoDTO, CrearCursoDTO, GrupoDTO } from '../../types';
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

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      setUsuarios(await usuarioService.listar());
    } catch { showAlert('error', 'Error al cargar usuarios'); }
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
    } catch { showAlert('error', 'Error al eliminar usuario'); }
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
    } catch { showAlert('error', 'Error al cargar roles'); }
  };

  const handleCursoChange = async (cursoId: number) => {
    setCursoSeleccionado(cursoId);
    setGrupoSeleccionado('');
    try {
      setGrupos(await cursoService.listarGrupos(cursoId));
    } catch { setGrupos([]); }
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
    } catch { showAlert('error', 'Error al quitar rol'); }
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
    } catch { showAlert('error', 'Error al quitar rol'); }
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
  const [profSeleccionado, setProfSeleccionado] = useState<number | ''>('');

  const cargar = useCallback(async () => {
    setLoading(true);
    try { setCursos(await cursoService.listarTodos()); }
    catch { showAlert('error', 'Error al cargar cursos'); }
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
    if (profSeleccionado === '') {
      showAlert('error', 'Selecciona un profesor para el curso');
      return;
    }
    try {
      // Check the user is a profesor and resolve the entity ID
      const isProf = await usuarioService.esProfesor(profSeleccionado as number);
      if (!isProf) {
        showAlert('error', 'El usuario seleccionado no tiene rol de profesor. Asígnalo primero desde Usuarios → Roles.');
        return;
      }
      const profId = await usuarioService.obtenerProfesorId(profSeleccionado as number);
      await cursoService.crear(profId, form);
      showAlert('success', 'Curso creado');
      setModalOpen(false);
      setCrearConProf(false);
      cargar();
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al crear curso');
    }
  };

  const openCrearConProf = async () => {
    setEditando(null);
    setForm({ titulo: '', descripcion: '', codigo: '' });
    setProfSeleccionado('');
    try {
      setUsuarios(await usuarioService.listar());
    } catch { /* ignore */ }
    setCrearConProf(true);
    setModalOpen(true);
  };

  const handleEliminar = async (id: number) => {
    if (!window.confirm('¿Eliminar este curso y todos sus datos?')) return;
    try {
      await cursoService.eliminar(id);
      showAlert('success', 'Curso eliminado');
      cargar();
    } catch { showAlert('error', 'Error al eliminar curso'); }
  };

  // Professor assignment modal
  const openProfModal = async (c: CursoDTO) => {
    setProfModal(c);
    setProfSeleccionado('');
    try { setUsuarios(await usuarioService.listar()); } catch { /* */ }
  };

  const handleAgregarProf = async () => {
    if (!profModal || profSeleccionado === '') return;
    try {
      const isProf = await usuarioService.esProfesor(profSeleccionado as number);
      if (!isProf) {
        showAlert('error', 'El usuario no tiene rol de profesor. Asígnalo primero desde la pestaña Usuarios → Roles.');
        return;
      }
      const profId = await usuarioService.obtenerProfesorId(profSeleccionado as number);
      await cursoService.agregarProfesor(profModal.id, profId);
      showAlert('success', 'Profesor asignado al curso');
      cargar();
    } catch { showAlert('error', 'Error al asignar profesor'); }
  };

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
                <input type="text" required value={form.titulo}
                  onChange={e => setForm({ ...form, titulo: e.target.value })} />
              </div>
              <div className="admin-field">
                <label>Código *</label>
                <input type="text" required value={form.codigo}
                  onChange={e => setForm({ ...form, codigo: e.target.value })} />
              </div>
              <div className="admin-field">
                <label>Descripción</label>
                <textarea value={form.descripcion || ''}
                  onChange={e => setForm({ ...form, descripcion: e.target.value })} rows={3} />
              </div>
              {crearConProf && !editando && (
                <div className="admin-field">
                  <label>Profesor responsable *</label>
                  <select value={profSeleccionado} onChange={e => setProfSeleccionado(Number(e.target.value))}>
                    <option value="">Selecciona un profesor...</option>
                    {usuarios.map(u => <option key={u.id} value={u.id}>{u.nombre} ({u.correoElectronico})</option>)}
                  </select>
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
              <div className="admin-role-enroll">
                <select value={profSeleccionado} onChange={e => setProfSeleccionado(Number(e.target.value))}>
                  <option value="">Selecciona un usuario...</option>
                  {usuarios.filter(u => !u.esAdmin).map(u =>
                    <option key={u.id} value={u.id}>{u.nombre} ({u.correoElectronico})</option>
                  )}
                </select>
                <button className="admin-btn admin-btn-primary" disabled={profSeleccionado === ''} onClick={handleAgregarProf}>
                  Asignar profesor
                </button>
              </div>
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
  const [cursoSeleccionado, setCursoSeleccionado] = useState<number | ''>('');
  const [grupos, setGrupos] = useState<GrupoDTO[]>([]);
  const [loading, setLoading] = useState(false);

  // Create / edit grupo
  const [modalOpen, setModalOpen] = useState(false);
  const [editandoGrupo, setEditandoGrupo] = useState<GrupoDTO | null>(null);
  const [tituloGrupo, setTituloGrupo] = useState('');

  // Estudiantes de un grupo
  const [estModal, setEstModal] = useState<GrupoDTO | null>(null);
  const [estudiantes, setEstudiantes] = useState<UsuarioDTO[]>([]);
  const [todosUsuarios, setTodosUsuarios] = useState<UsuarioDTO[]>([]);
  const [estSeleccionado, setEstSeleccionado] = useState<number | ''>('');

  useEffect(() => {
    cursoService.listarTodos().then(setCursos).catch(() => {});
  }, []);

  const cargarGrupos = useCallback(async (cursoId: number) => {
    setLoading(true);
    try { setGrupos(await cursoService.listarGrupos(cursoId)); }
    catch { showAlert('error', 'Error al cargar grupos'); }
    finally { setLoading(false); }
  }, [showAlert]);

  const handleCursoChange = (cid: number) => {
    setCursoSeleccionado(cid);
    cargarGrupos(cid);
  };

  const openCrearGrupo = () => {
    if (cursoSeleccionado === '') {
      showAlert('error', 'Selecciona un curso primero');
      return;
    }
    setEditandoGrupo(null);
    setTituloGrupo('');
    setModalOpen(true);
  };

  const openEditarGrupo = (g: GrupoDTO) => {
    setEditandoGrupo(g);
    setTituloGrupo(g.titulo);
    setModalOpen(true);
  };

  const handleSubmitGrupo = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editandoGrupo) {
        await cursoService.actualizarGrupo(editandoGrupo.id, tituloGrupo);
        showAlert('success', 'Grupo actualizado');
      } else {
        await cursoService.crearGrupo(cursoSeleccionado as number, tituloGrupo);
        showAlert('success', 'Grupo creado');
      }
      setModalOpen(false);
      if (cursoSeleccionado !== '') cargarGrupos(cursoSeleccionado as number);
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al guardar grupo');
    }
  };

  const handleEliminarGrupo = async (gid: number) => {
    if (!window.confirm('¿Eliminar este grupo?')) return;
    try {
      await cursoService.eliminarGrupo(gid);
      showAlert('success', 'Grupo eliminado');
      if (cursoSeleccionado !== '') cargarGrupos(cursoSeleccionado as number);
    } catch { showAlert('error', 'Error al eliminar grupo'); }
  };

  // Estudiantes modal
  const openEstModal = async (g: GrupoDTO) => {
    setEstModal(g);
    setEstSeleccionado('');
    try {
      const [ests, users] = await Promise.all([
        usuarioService.listarEstudiantesDeGrupo(g.id),
        usuarioService.listar()
      ]);
      setEstudiantes(ests);
      setTodosUsuarios(users);
    } catch { showAlert('error', 'Error al cargar estudiantes'); }
  };

  const handleAgregarEstudiante = async () => {
    if (!estModal || estSeleccionado === '') return;
    try {
      await usuarioService.registrarComoEstudiante(estSeleccionado as number, estModal.id);
      showAlert('success', 'Estudiante inscrito');
      // Recargar
      setEstudiantes(await usuarioService.listarEstudiantesDeGrupo(estModal.id));
      setEstSeleccionado('');
      if (cursoSeleccionado !== '') cargarGrupos(cursoSeleccionado as number);
    } catch (err: unknown) {
      const axErr = err as { response?: { data?: { message?: string } } };
      showAlert('error', axErr?.response?.data?.message || 'Error al inscribir estudiante');
    }
  };

  const handleQuitarEstudiante = async (usuarioId: number) => {
    if (!estModal) return;
    try {
      await usuarioService.eliminarEstudianteDeGrupo(usuarioId, estModal.id);
      showAlert('success', 'Estudiante eliminado del grupo');
      setEstudiantes(await usuarioService.listarEstudiantesDeGrupo(estModal.id));
      if (cursoSeleccionado !== '') cargarGrupos(cursoSeleccionado as number);
    } catch { showAlert('error', 'Error al quitar estudiante'); }
  };

  return (
    <div className="admin-tab-content">
      <div className="admin-toolbar">
        <div className="admin-search">
          <select value={cursoSeleccionado} onChange={e => handleCursoChange(Number(e.target.value))}
            className="admin-curso-select">
            <option value="">Selecciona un curso...</option>
            {cursos.map(c => <option key={c.id} value={c.id}>{c.titulo} ({c.codigo})</option>)}
          </select>
        </div>
        <button className="admin-btn admin-btn-primary" onClick={openCrearGrupo}
          disabled={cursoSeleccionado === ''}>
          + Nuevo Grupo
        </button>
      </div>

      {cursoSeleccionado === '' ? (
        <div className="admin-empty-state">
          <p>Selecciona un curso para ver sus grupos</p>
        </div>
      ) : loading ? (
        <div className="admin-loading">Cargando grupos...</div>
      ) : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Título</th>
                <th>Curso</th>
                <th>Estudiantes</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {grupos.length === 0 ? (
                <tr><td colSpan={5} className="admin-empty">No hay grupos en este curso</td></tr>
              ) : grupos.map(g => (
                <tr key={g.id}>
                  <td className="admin-td-id">{g.id}</td>
                  <td>{g.titulo}</td>
                  <td>{g.cursoTitulo}</td>
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
              <div className="admin-field">
                <label>Título del grupo *</label>
                <input type="text" required value={tituloGrupo}
                  onChange={e => setTituloGrupo(e.target.value)} placeholder="Ej: Grupo A" />
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
              {/* Añadir estudiante */}
              <div className="admin-role-enroll">
                <select value={estSeleccionado} onChange={e => setEstSeleccionado(Number(e.target.value))}>
                  <option value="">Selecciona un usuario...</option>
                  {todosUsuarios
                    .filter(u => !estudiantes.some(es => es.id === u.id))
                    .map(u => <option key={u.id} value={u.id}>{u.nombre} ({u.correoElectronico})</option>)
                  }
                </select>
                <button className="admin-btn admin-btn-primary" disabled={estSeleccionado === ''} onClick={handleAgregarEstudiante}>
                  Inscribir
                </button>
              </div>

              {/* Lista de estudiantes */}
              {estudiantes.length === 0 ? (
                <p className="admin-info-text">No hay estudiantes en este grupo.</p>
              ) : (
                <table className="admin-table admin-table-inner">
                  <thead>
                    <tr>
                      <th>Nombre</th>
                      <th>Correo</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {estudiantes.map(est => (
                      <tr key={est.id}>
                        <td>{est.nombre}</td>
                        <td className="admin-td-email">{est.correoElectronico}</td>
                        <td>
                          <button className="admin-btn-sm admin-btn-delete"
                            onClick={() => handleQuitarEstudiante(est.id)} title="Quitar del grupo">
                            ✕
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPage;
