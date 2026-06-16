import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { api } from "../api/api";
// @ts-ignore
import "./AdminQuestionsManagePage.css";

type QuestionDto = {
  id: number;
  text: string;
  answerType: string;
  category: string;
  level: string;
  languages: string[];
  positions: string[];
  active: boolean;
  testCaseCount: number;
};

type Filters = {
  search: string;
  category: string;
  level: string;
  answerType: string;
  language: string;
  active: string; // "all" | "true" | "false"
};

const CATEGORY_COLORS: Record<string, string> = {
  HR: "#0891b2", TECH: "#6d28d9", PROBLEM: "#059669",
};
const TYPE_LABELS: Record<string, string> = {
  TEXT: "HR Text", MCQ: "Grilă", CODE: "Cod",
};
const TYPE_COLORS: Record<string, string> = {
  TEXT: "#0891b2", MCQ: "#6d28d9", CODE: "#059669",
};

export default function AdminQuestionsManagePage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState("");

  const [filters, setFilters] = useState<Filters>({
    search: "", category: "", level: "", answerType: "", language: "", active: "all",
  });

  const [editingId, setEditingId]   = useState<number | null>(null);
  const [editText, setEditText]     = useState("");
  const [savingId, setSavingId]     = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmId, setConfirmId]   = useState<number | null>(null);
  const [togglingId, setTogglingId] = useState<number | null>(null);

  useEffect(() => { loadQuestions(); }, []);

  function loadQuestions() {
    setLoading(true);
    const params = new URLSearchParams();
    if (filters.category)   params.set("category",   filters.category);
    if (filters.level)      params.set("level",      filters.level);
    if (filters.answerType) params.set("answerType", filters.answerType);
    if (filters.language)   params.set("language",   filters.language);
    if (filters.active !== "all") params.set("active", filters.active);

    api.get<QuestionDto[]>(`/api/admin/questions?${params}`)
      .then(r => setQuestions(r.data))
      .catch(() => setError("Nu s-au putut încărca întrebările."))
      .finally(() => setLoading(false));
  }

  function setFilter(key: keyof Filters, val: string) {
    setFilters(prev => ({ ...prev, [key]: val }));
  }

  // Client-side search filter
  const filtered = questions.filter(q => {
    if (!filters.search) return true;
    const s = filters.search.toLowerCase();
    return q.text.toLowerCase().includes(s) ||
           q.category.toLowerCase().includes(s) ||
           q.level.toLowerCase().includes(s) ||
           q.languages.some(l => l.toLowerCase().includes(s));
  });

  // Unique values for filter dropdowns (from loaded data)
  const levels    = [...new Set(questions.map(q => q.level))].sort();
  const languages = [...new Set(questions.flatMap(q => q.languages))].sort();

  async function handleToggleActive(q: QuestionDto) {
    setTogglingId(q.id);
    try {
      await api.patch(`/api/admin/questions/${q.id}/active`);
      setQuestions(prev => prev.map(x => x.id === q.id ? { ...x, active: !x.active } : x));
    } catch {
      alert("Eroare la modificarea statusului.");
    } finally {
      setTogglingId(null);
    }
  }

  function startEdit(q: QuestionDto) {
    setEditingId(q.id);
    setEditText(q.text);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditText("");
  }

  async function handleSaveEdit(id: number) {
    if (!editText.trim()) return;
    setSavingId(id);
    try {
      const res = await api.put<QuestionDto>(`/api/admin/questions/${id}`, { text: editText.trim() });
      setQuestions(prev => prev.map(q => q.id === id ? res.data : q));
      setEditingId(null);
    } catch {
      alert("Eroare la salvare.");
    } finally {
      setSavingId(null);
    }
  }

  async function handleDelete(id: number) {
    setDeletingId(id);
    try {
      await api.delete(`/api/admin/questions/${id}`);
      setQuestions(prev => prev.filter(q => q.id !== id));
      setConfirmId(null);
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

  const activeCount   = questions.filter(q => q.active).length;
  const inactiveCount = questions.filter(q => !q.active).length;

  return (
    <div className="adm-layout">
      {/* ── Sidebar ── */}
      <aside className="adm-sidebar">
        <div className="adm-sidebar-logo">
          <span className="adm-logo-icon">⚡</span>
          <span className="adm-logo-text">Admin Panel</span>
        </div>
        <nav className="adm-nav">
          <button className="adm-nav-item" onClick={() => navigate("/admin")}>
            <span className="adm-nav-icon">📊</span><span>Dashboard</span>
          </button>
          <button className="adm-nav-item" onClick={() => navigate("/admin/users")}>
            <span className="adm-nav-icon">👥</span><span>Utilizatori</span>
          </button>
          <button className="adm-nav-item adm-nav-item-active">
            <span className="adm-nav-icon">📋</span><span>Întrebări</span>
          </button>
          <button className="adm-nav-item" onClick={() => navigate("/admin/questions")}>
            <span className="adm-nav-icon">➕</span><span>Adaugă întrebare</span>
          </button>
        </nav>
        <div className="adm-sidebar-footer">
          <button className="adm-nav-item" onClick={() => navigate("/")}>
            <span className="adm-nav-icon">🏠</span><span>Înapoi la app</span>
          </button>
          <button className="adm-nav-item adm-logout-btn" onClick={handleLogout}>
            <span className="adm-nav-icon">🚪</span><span>Deconectare</span>
          </button>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className="adm-main">
        <header className="adm-topbar">
          <div>
            <h1 className="adm-topbar-title">Gestionare întrebări</h1>
            <p className="adm-topbar-sub">
              {questions.length} întrebări · {activeCount} active · {inactiveCount} inactive
            </p>
          </div>
          <div className="adm-topbar-right">
            <button className="btn btn-primary" onClick={() => navigate("/admin/questions")} style={{ fontSize: 13 }}>
              + Adaugă întrebare
            </button>
            <span className="badge badge-purple">ADMIN</span>
          </div>
        </header>

        <div className="adm-content">
          {error && <div className="error-box" style={{ marginBottom: 20 }}>{error}</div>}

          {/* ── Filters ── */}
          <div className="card aqm-filters">
            <div className="aqm-search-wrap">
              <span className="aqm-search-icon">🔍</span>
              <input
                className="aqm-search"
                placeholder="Caută în textul întrebării..."
                value={filters.search}
                onChange={e => setFilter("search", e.target.value)}
              />
              {filters.search && (
                <button className="aqm-search-clear" onClick={() => setFilter("search", "")}>✕</button>
              )}
            </div>
            <div className="aqm-selects">
              <select className="aqm-select" value={filters.category} onChange={e => setFilter("category", e.target.value)}>
                <option value="">Toate categoriile</option>
                <option value="HR">HR</option>
                <option value="TECH">TECH</option>
                <option value="PROBLEM">PROBLEM</option>
              </select>
              <select className="aqm-select" value={filters.answerType} onChange={e => setFilter("answerType", e.target.value)}>
                <option value="">Toate tipurile</option>
                <option value="TEXT">TEXT (HR)</option>
                <option value="MCQ">MCQ (Grilă)</option>
                <option value="CODE">CODE (Cod)</option>
              </select>
              <select className="aqm-select" value={filters.level} onChange={e => setFilter("level", e.target.value)}>
                <option value="">Toate nivelurile</option>
                {levels.map(l => <option key={l} value={l}>{l}</option>)}
              </select>
              <select className="aqm-select" value={filters.language} onChange={e => setFilter("language", e.target.value)}>
                <option value="">Toate limbajele</option>
                {languages.map(l => <option key={l} value={l}>{l}</option>)}
              </select>
              <select className="aqm-select" value={filters.active} onChange={e => setFilter("active", e.target.value)}>
                <option value="all">Active + Inactive</option>
                <option value="true">Doar active</option>
                <option value="false">Doar inactive</option>
              </select>
              <button className="btn btn-outline aqm-apply-btn" onClick={loadQuestions} style={{ fontSize: 13 }}>
                Aplică filtrele
              </button>
            </div>
          </div>

          {/* ── Table ── */}
          {loading ? (
            <div className="adm-loading">Se încarcă întrebările...</div>
          ) : (
            <div className="card aqm-table-card">
              <div className="aqm-table-header">
                <span className="aqm-table-count">{filtered.length} rezultate</span>
              </div>
              <div className="aqm-table-wrap">
                <table className="aqm-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Întrebare</th>
                      <th>Tip</th>
                      <th>Categorie</th>
                      <th>Nivel</th>
                      <th>Limbaje</th>
                      <th>Tests</th>
                      <th>Status</th>
                      <th>Acțiuni</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.length === 0 ? (
                      <tr><td colSpan={9} className="aqm-empty">Nicio întrebare găsită.</td></tr>
                    ) : filtered.map((q, idx) => (
                      <tr key={q.id} className={!q.active ? "aqm-row-inactive" : ""}>
                        <td className="aqm-id-cell">#{q.id}</td>

                        {/* Text — editable */}
                        <td className="aqm-text-cell">
                          {editingId === q.id ? (
                            <div className="aqm-edit-wrap">
                              <textarea
                                className="aqm-edit-textarea"
                                value={editText}
                                onChange={e => setEditText(e.target.value)}
                                rows={3}
                                autoFocus
                              />
                              <div className="aqm-edit-actions">
                                <button
                                  className="aqm-btn aqm-btn-save"
                                  disabled={savingId === q.id}
                                  onClick={() => handleSaveEdit(q.id)}
                                >
                                  {savingId === q.id ? "..." : "✓ Salvează"}
                                </button>
                                <button className="aqm-btn aqm-btn-cancel" onClick={cancelEdit}>
                                  Anulează
                                </button>
                              </div>
                            </div>
                          ) : (
                            <span className="aqm-text">{q.text}</span>
                          )}
                        </td>

                        {/* Type */}
                        <td>
                          <span
                            className="aqm-type-badge"
                            style={{ background: `${TYPE_COLORS[q.answerType]}18`, color: TYPE_COLORS[q.answerType], borderColor: `${TYPE_COLORS[q.answerType]}35` }}
                          >
                            {TYPE_LABELS[q.answerType] ?? q.answerType}
                          </span>
                        </td>

                        {/* Category */}
                        <td>
                          <span
                            className="aqm-cat-badge"
                            style={{ background: `${CATEGORY_COLORS[q.category]}15`, color: CATEGORY_COLORS[q.category], borderColor: `${CATEGORY_COLORS[q.category]}30` }}
                          >
                            {q.category}
                          </span>
                        </td>

                        {/* Level */}
                        <td><span className="badge badge-gray">{q.level}</span></td>

                        {/* Languages */}
                        <td>
                          <div className="aqm-lang-list">
                            {q.languages.map(l => (
                              <span key={l} className="aqm-lang-chip">{l}</span>
                            ))}
                          </div>
                        </td>

                        {/* Test cases count */}
                        <td className="aqm-center">
                          {q.answerType === "CODE"
                            ? <span className="aqm-tc-count">{q.testCaseCount}</span>
                            : <span className="aqm-na">—</span>}
                        </td>

                        {/* Active status */}
                        <td className="aqm-center">
                          <button
                            className={`aqm-toggle ${q.active ? "aqm-toggle-on" : "aqm-toggle-off"}`}
                            disabled={togglingId === q.id}
                            onClick={() => handleToggleActive(q)}
                            title={q.active ? "Dezactivează" : "Activează"}
                          >
                            {togglingId === q.id ? "..." : q.active ? "Activ" : "Inactiv"}
                          </button>
                        </td>

                        {/* Actions */}
                        <td>
                          <div className="aqm-actions">
                            {editingId !== q.id && (
                              <button
                                className="aqm-btn aqm-btn-edit"
                                onClick={() => startEdit(q)}
                                title="Editează textul"
                              >
                                ✎ Edit
                              </button>
                            )}
                            {confirmId === q.id ? (
                              <div className="aqm-confirm">
                                <button
                                  className="aqm-btn aqm-btn-delete-confirm"
                                  disabled={deletingId === q.id}
                                  onClick={() => handleDelete(q.id)}
                                >
                                  {deletingId === q.id ? "..." : "Șterge"}
                                </button>
                                <button className="aqm-btn aqm-btn-cancel" onClick={() => setConfirmId(null)}>
                                  Nu
                                </button>
                              </div>
                            ) : (
                              <button
                                className="aqm-btn aqm-btn-delete"
                                onClick={() => setConfirmId(q.id)}
                                title="Șterge întrebarea"
                              >
                                🗑
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
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
