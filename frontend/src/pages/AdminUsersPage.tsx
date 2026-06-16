import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { api } from "../api/api";
// @ts-ignore
import "./AdminUsersPage.css";

type UserDto = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  interviewCount: number;
  completedCount: number;
  avgScore: number | null;
  createdAt: string | null;
};

type FilterRole = "all" | "admin" | "user";

export default function AdminUsersPage() {
  const navigate = useNavigate();
  const { logout, state } = useAuth();

  const [users, setUsers]       = useState<UserDto[]>([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState("");
  const [filterRole, setFilterRole] = useState<FilterRole>("all");
  const [search, setSearch]     = useState("");
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);

  useEffect(() => {
    api.get<UserDto[]>("/api/admin/users")
      .then(r => setUsers(r.data))
      .catch(() => setError("Nu s-au putut încărca utilizatorii."))
      .finally(() => setLoading(false));
  }, []);

  const currentEmail = state.user?.email;

  const filtered = users.filter(u => {
    const matchRole =
      filterRole === "all" ? true :
      filterRole === "admin" ? u.roles.includes("ADMIN") :
      !u.roles.includes("ADMIN");

    const q = search.toLowerCase();
    const matchSearch = !q ||
      u.email.toLowerCase().includes(q) ||
      (u.firstName + " " + u.lastName).toLowerCase().includes(q);

    return matchRole && matchSearch;
  });

  async function handleToggleAdmin(u: UserDto) {
    const makeAdmin = !u.roles.includes("ADMIN");
    setTogglingId(u.id);
    try {
      await api.put(`/api/admin/users/${u.id}/role`, { admin: makeAdmin });
      setUsers(prev => prev.map(x =>
        x.id !== u.id ? x : {
          ...x,
          roles: makeAdmin
            ? [...x.roles.filter(r => r !== "USER"), "ADMIN"]
            : x.roles.filter(r => r !== "ADMIN").concat("USER"),
        }
      ));
    } catch (err: any) {
      alert(err?.response?.data?.message || "Eroare la modificarea rolului.");
    } finally {
      setTogglingId(null);
    }
  }

  async function handleDelete(id: number) {
    setDeletingId(id);
    try {
      await api.delete(`/api/admin/users/${id}`);
      setUsers(prev => prev.filter(u => u.id !== id));
      setConfirmDelete(null);
    } catch (err: any) {
      alert(err?.response?.data?.message || "Eroare la ștergere.");
    } finally {
      setDeletingId(null);
    }
  }

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  const totalUsers  = users.filter(u => !u.roles.includes("ADMIN")).length;
  const totalAdmins = users.filter(u => u.roles.includes("ADMIN")).length;

  return (
    <div className="adm-layout">
      {/* ── Sidebar ── */}
      <aside className="adm-sidebar">
        <div className="adm-sidebar-logo">
          <span className="adm-logo-text">Admin Panel</span>
        </div>
        <nav className="adm-nav">
          <button className="adm-nav-item" onClick={() => navigate("/admin")}>
            <span>Dashboard</span>
          </button>
          <button className="adm-nav-item adm-nav-item-active">
            <span>Utilizatori</span>
          </button>
          <button className="adm-nav-item" onClick={() => navigate("/admin/questions/manage")}>
            <span>Întrebări</span>
          </button>
          <button className="adm-nav-item" onClick={() => navigate("/admin/questions")}>
            <span>Adaugă întrebare</span>
          </button>
        </nav>
        <div className="adm-sidebar-footer">
          <button className="adm-nav-item" onClick={() => navigate("/")}>
            <span>Înapoi la app</span>
          </button>
          <button className="adm-nav-item adm-logout-btn" onClick={handleLogout}>
            <span>Deconectare</span>
          </button>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className="adm-main">
        <header className="adm-topbar">
          <div>
            <h1 className="adm-topbar-title">Utilizatori</h1>
            <p className="adm-topbar-sub">Gestionează conturile și drepturile de acces</p>
          </div>
          <div className="adm-topbar-right">
            <div className="aus-summary">
              <span className="aus-summary-item">
                <span className="aus-num">{totalUsers}</span> utilizatori
              </span>
              <span className="aus-summary-sep">·</span>
              <span className="aus-summary-item">
                <span className="aus-num">{totalAdmins}</span> admini
              </span>
            </div>
            <span className="badge badge-purple">ADMIN</span>
          </div>
        </header>

        <div className="adm-content">
          {error && <div className="error-box" style={{ marginBottom: 20 }}>{error}</div>}

          {/* ── Filters ── */}
          <div className="aus-filters card">
            <div className="aus-search-wrap">
              <input
                className="aus-search"
                placeholder="Caută după email sau nume..."
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
              {search && (
                <button className="aus-search-clear" onClick={() => setSearch("")}>✕</button>
              )}
            </div>
            <div className="aus-role-tabs">
              {(["all", "user", "admin"] as FilterRole[]).map(f => (
                <button
                  key={f}
                  className={`aus-role-tab ${filterRole === f ? "aus-role-tab-active" : ""}`}
                  onClick={() => setFilterRole(f)}
                >
                  {f === "all" ? "Toți" : f === "admin" ? "Admini" : "Utilizatori"}
                </button>
              ))}
            </div>
          </div>

          {/* ── Table ── */}
          {loading ? (
            <div className="adm-loading">Se încarcă utilizatorii...</div>
          ) : (
            <div className="card aus-table-card">
              <div className="aus-table-header">
                <span className="aus-table-count">{filtered.length} rezultate</span>
              </div>
              <div className="aus-table-wrap">
                <table className="aus-table">
                  <thead>
                    <tr>
                      <th>Utilizator</th>
                      <th>Rol</th>
                      <th>Interviuri</th>
                      <th>Finalizate</th>
                      <th>Scor mediu</th>
                      <th>Cont creat</th>
                      <th>Acțiuni</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="aus-empty">Niciun utilizator găsit.</td>
                      </tr>
                    ) : filtered.map(u => {
                      const isAdmin   = u.roles.includes("ADMIN");
                      const isSelf    = u.email === currentEmail;
                      const fullName  = [u.firstName, u.lastName].filter(Boolean).join(" ");

                      return (
                        <tr key={u.id} className={isSelf ? "aus-row-self" : ""}>
                          {/* User info */}
                          <td>
                            <div className="aus-user-cell">
                              <div className="aus-avatar" style={{ background: isAdmin ? "linear-gradient(135deg,#6d28d9,#0891b2)" : "linear-gradient(135deg,#0891b2,#059669)" }}>
                                {(u.firstName?.[0] || u.email[0]).toUpperCase()}
                              </div>
                              <div>
                                <div className="aus-name">
                                  {fullName || "—"}
                                  {isSelf && <span className="aus-self-badge">tu</span>}
                                </div>
                                <div className="aus-email">{u.email}</div>
                              </div>
                            </div>
                          </td>

                          {/* Role */}
                          <td>
                            {isAdmin
                              ? <span className="badge badge-purple">ADMIN</span>
                              : <span className="badge badge-gray">USER</span>
                            }
                          </td>

                          {/* Stats */}
                          <td className="aus-num-cell">{u.interviewCount}</td>
                          <td className="aus-num-cell">{u.completedCount}</td>
                          <td className="aus-num-cell">
                            {u.avgScore !== null
                              ? <span className={`aus-score ${u.avgScore >= 70 ? "aus-score-good" : u.avgScore >= 40 ? "aus-score-mid" : "aus-score-low"}`}>
                                  {u.avgScore}%
                                </span>
                              : <span className="aus-score-none">—</span>
                            }
                          </td>

                          {/* Created at */}
                          <td className="aus-date-cell">
                            {u.createdAt
                              ? new Date(u.createdAt).toLocaleDateString("ro-RO")
                              : "—"}
                          </td>

                          {/* Actions */}
                          <td>
                            <div className="aus-actions">
                              {!isSelf && (
                                <>
                                  <button
                                    className={`aus-action-btn ${isAdmin ? "aus-btn-demote" : "aus-btn-promote"}`}
                                    disabled={togglingId === u.id}
                                    onClick={() => handleToggleAdmin(u)}
                                    title={isAdmin ? "Retrage rol Admin" : "Fă Admin"}
                                  >
                                    {togglingId === u.id
                                      ? "..."
                                      : isAdmin ? "↓ Retrage Admin" : "↑ Fă Admin"
                                    }
                                  </button>
                                  {!isAdmin && (
                                    confirmDelete === u.id ? (
                                      <div className="aus-confirm">
                                        <span className="aus-confirm-text">Sigur?</span>
                                        <button
                                          className="aus-action-btn aus-btn-delete-confirm"
                                          disabled={deletingId === u.id}
                                          onClick={() => handleDelete(u.id)}
                                        >
                                          {deletingId === u.id ? "..." : "Da, șterge"}
                                        </button>
                                        <button
                                          className="aus-action-btn aus-btn-cancel"
                                          onClick={() => setConfirmDelete(null)}
                                        >
                                          Anulează
                                        </button>
                                      </div>
                                    ) : (
                                      <button
                                        className="aus-action-btn aus-btn-delete"
                                        onClick={() => setConfirmDelete(u.id)}
                                        title="Șterge cont"
                                      >
                                        Șterge
                                      </button>
                                    )
                                  )}
                                </>
                              )}
                              {isSelf && <span className="aus-self-label">—</span>}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
