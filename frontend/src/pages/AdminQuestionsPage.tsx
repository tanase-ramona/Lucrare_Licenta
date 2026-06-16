import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { api } from "../api/api";
// @ts-ignore
import "./AdminQuestionsPage.css";

type FilterItem = { id: number; name: string };

type QuestionType = "TEXT" | "MCQ" | "CODE";

type OptionForm = { text: string; correct: boolean };
type TestCaseForm = { description: string; inputData: string; expectedOutput: string };

const TYPE_OPTIONS: { value: QuestionType; label: string; category: string; description: string }[] = [
  { value: "TEXT",  label: "HR",            category: "HR",      description: "Întrebare cu răspuns liber — comportament, motivație" },
  { value: "MCQ",   label: "Tehnic grilă",  category: "TECH",    description: "Întrebare cu variante de răspuns, o singură variantă corectă" },
  { value: "CODE",  label: "Cod",           category: "PROBLEM", description: "Problemă de programare cu test cases de validare" },
];

export default function AdminQuestionsPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [levels, setLevels]       = useState<FilterItem[]>([]);
  const [positions, setPositions] = useState<FilterItem[]>([]);
  const [languages, setLanguages] = useState<FilterItem[]>([]);

  const [questionType, setQuestionType] = useState<QuestionType>("TEXT");
  const [text, setText]                 = useState("");
  const [levelId, setLevelId]           = useState<number | "">("");
  const [selectedLangs, setSelectedLangs]   = useState<number[]>([]);
  const [selectedPos, setSelectedPos]       = useState<number[]>([]);
  const [starterCode, setStarterCode]       = useState("");
  const [options, setOptions]               = useState<OptionForm[]>([
    { text: "", correct: false },
    { text: "", correct: false },
    { text: "", correct: false },
    { text: "", correct: false },
  ]);
  const [testCases, setTestCases] = useState<TestCaseForm[]>([
    { description: "", inputData: "", expectedOutput: "" },
  ]);

  const [saving, setSaving]   = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError]     = useState("");

  useEffect(() => {
    api.get<{ levels: FilterItem[]; positions: FilterItem[]; languages: FilterItem[] }>("/api/interview/filters")
      .then(r => {
        setLevels(r.data.levels);
        setPositions(r.data.positions);
        setLanguages(r.data.languages);
      })
      .catch(() => setError("Nu s-au putut încărca filtrele."));
  }, []);

  function toggleLang(id: number) {
    setSelectedLangs(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  }

  function togglePos(id: number) {
    setSelectedPos(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  }

  function setOptionText(idx: number, val: string) {
    setOptions(prev => prev.map((o, i) => i === idx ? { ...o, text: val } : o));
  }

  function setCorrectOption(idx: number) {
    setOptions(prev => prev.map((o, i) => ({ ...o, correct: i === idx })));
  }

  function addTestCase() {
    setTestCases(prev => [...prev, { description: "", inputData: "", expectedOutput: "" }]);
  }

  function removeTestCase(idx: number) {
    setTestCases(prev => prev.filter((_, i) => i !== idx));
  }

  function updateTestCase(idx: number, field: keyof TestCaseForm, val: string) {
    setTestCases(prev => prev.map((tc, i) => i === idx ? { ...tc, [field]: val } : tc));
  }

  function resetForm() {
    setText("");
    setLevelId("");
    setSelectedLangs([]);
    setSelectedPos([]);
    setStarterCode("");
    setOptions([
      { text: "", correct: false },
      { text: "", correct: false },
      { text: "", correct: false },
      { text: "", correct: false },
    ]);
    setTestCases([{ description: "", inputData: "", expectedOutput: "" }]);
  }

  async function handleSubmit() {
    setError("");
    setSuccess("");

    if (!text.trim()) { setError("Textul întrebării este obligatoriu."); return; }
    if (!levelId)      { setError("Selectează nivelul."); return; }
    if (selectedPos.length === 0) { setError("Selectează cel puțin o poziție."); return; }
    if (questionType !== "TEXT" && selectedLangs.length === 0) {
      setError("Selectează cel puțin un limbaj.");
      return;
    }
    if (questionType === "MCQ") {
      const filled = options.filter(o => o.text.trim());
      if (filled.length < 2) { setError("Adaugă cel puțin 2 variante de răspuns."); return; }
      if (!options.some(o => o.correct)) { setError("Marchează varianta corectă."); return; }
    }
    if (questionType === "CODE") {
      const validTc = testCases.filter(tc => tc.description.trim() && tc.expectedOutput.trim());
      if (validTc.length === 0) { setError("Adaugă cel puțin un test case cu descriere și output așteptat."); return; }
    }

    const selected = TYPE_OPTIONS.find(t => t.value === questionType)!;
    setSaving(true);
    try {
      const payload = {
        text: text.trim(),
        categoryName: selected.category,
        levelId,
        languageIds: selectedLangs,
        positionIds: selectedPos,
        answerType: questionType,
        starterCode: questionType === "CODE" ? starterCode : null,
        options: questionType === "MCQ"
          ? options.filter(o => o.text.trim()).map(o => ({ text: o.text, correct: o.correct }))
          : [],
      };

      const res = await api.post<{ id: number }>("/api/admin/questions", payload);
      const questionId = res.data.id;

      if (questionType === "CODE") {
        for (let i = 0; i < testCases.length; i++) {
          const tc = testCases[i];
          if (tc.description.trim() && tc.expectedOutput.trim()) {
            await api.post(`/api/admin/questions/${questionId}/testcases`, {
              description: tc.description,
              inputData: tc.inputData || null,
              expectedOutput: tc.expectedOutput,
              orderIndex: i,
            });
          }
        }
      }

      setSuccess("Întrebarea a fost adăugată cu succes!");
      resetForm();
    } catch (err: any) {
      setError(err?.response?.data?.message || "Eroare la salvare. Încearcă din nou.");
    } finally {
      setSaving(false);
    }
  }

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  const currentType = TYPE_OPTIONS.find(t => t.value === questionType)!;

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
            <span className="adm-nav-icon">📊</span>
            <span>Dashboard</span>
          </button>
          <button className="adm-nav-item adm-nav-item-active">
            <span className="adm-nav-icon">➕</span>
            <span>Adaugă întrebări</span>
          </button>
        </nav>

        <div className="adm-sidebar-footer">
          <button className="adm-nav-item" onClick={() => navigate("/")}>
            <span className="adm-nav-icon">🏠</span>
            <span>Înapoi la app</span>
          </button>
          <button className="adm-nav-item adm-logout-btn" onClick={handleLogout}>
            <span className="adm-nav-icon">🚪</span>
            <span>Deconectare</span>
          </button>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className="adm-main">
        <header className="adm-topbar">
          <div>
            <h1 className="adm-topbar-title">Adaugă întrebare</h1>
            <p className="adm-topbar-sub">Completează formularul pentru a adăuga o nouă întrebare în baza de date</p>
          </div>
          <span className="badge badge-purple">ADMIN</span>
        </header>

        <div className="adm-content">
          {error   && <div className="error-box" style={{ marginBottom: 20 }}>{error}</div>}
          {success && <div className="aq-success">{success}</div>}

          <div className="aq-form card">

            {/* ── Tip întrebare ── */}
            <div className="aq-section">
              <h2 className="aq-section-title">Tipul întrebării</h2>
              <div className="aq-type-grid">
                {TYPE_OPTIONS.map(t => (
                  <button
                    key={t.value}
                    type="button"
                    className={`aq-type-card ${questionType === t.value ? "aq-type-card-active" : ""}`}
                    onClick={() => setQuestionType(t.value)}
                  >
                    <span className="aq-type-label">{t.label}</span>
                    <span className="aq-type-desc">{t.description}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* ── Textul întrebării ── */}
            <div className="aq-section">
              <h2 className="aq-section-title">Textul întrebării</h2>
              <textarea
                className="aq-textarea"
                rows={4}
                placeholder={`Scrie textul întrebării de tip ${currentType.label}...`}
                value={text}
                onChange={e => setText(e.target.value)}
              />
            </div>

            {/* ── Nivel + Poziții + Limbaje ── */}
            <div className="aq-section">
              <h2 className="aq-section-title">Configurare</h2>
              <div className="aq-config-grid">

                <div className="aq-field">
                  <label className="aq-label">Nivel de dificultate *</label>
                  <select
                    className="aq-select"
                    value={levelId}
                    onChange={e => setLevelId(Number(e.target.value))}
                  >
                    <option value="">Selectează nivelul</option>
                    {levels.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
                  </select>
                </div>

                <div className="aq-field">
                  <label className="aq-label">Poziții *</label>
                  <div className="aq-chips">
                    {positions.map(p => (
                      <button
                        key={p.id}
                        type="button"
                        className={`aq-chip ${selectedPos.includes(p.id) ? "aq-chip-active" : ""}`}
                        onClick={() => togglePos(p.id)}
                      >
                        {p.name}
                      </button>
                    ))}
                  </div>
                </div>

                {questionType !== "TEXT" && (
                  <div className="aq-field">
                    <label className="aq-label">Limbaje *</label>
                    <div className="aq-chips">
                      {languages.map(l => (
                        <button
                          key={l.id}
                          type="button"
                          className={`aq-chip ${selectedLangs.includes(l.id) ? "aq-chip-active" : ""}`}
                          onClick={() => toggleLang(l.id)}
                        >
                          {l.name}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* ── Variante răspuns (MCQ) ── */}
            {questionType === "MCQ" && (
              <div className="aq-section">
                <h2 className="aq-section-title">Variante de răspuns</h2>
                <p className="aq-hint">Completează variantele și selectează cea corectă.</p>
                <div className="aq-options">
                  {options.map((opt, i) => (
                    <div key={i} className="aq-option-row">
                      <input
                        type="radio"
                        name="correct"
                        checked={opt.correct}
                        onChange={() => setCorrectOption(i)}
                        className="aq-radio"
                        title="Marchează ca răspuns corect"
                      />
                      <input
                        type="text"
                        className="aq-option-input"
                        placeholder={`Varianta ${String.fromCharCode(65 + i)}`}
                        value={opt.text}
                        onChange={e => setOptionText(i, e.target.value)}
                      />
                      {opt.correct && <span className="aq-correct-badge">Corect</span>}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* ── Cod (CODE) ── */}
            {questionType === "CODE" && (
              <>
                <div className="aq-section">
                  <h2 className="aq-section-title">Cod de pornire (opțional)</h2>
                  <p className="aq-hint">Scheletul de cod oferit candidatului ca punct de plecare.</p>
                  <textarea
                    className="aq-textarea aq-code"
                    rows={6}
                    placeholder="// scrie codul de pornire aici..."
                    value={starterCode}
                    onChange={e => setStarterCode(e.target.value)}
                  />
                </div>

                <div className="aq-section">
                  <div className="aq-section-header">
                    <h2 className="aq-section-title">Test cases</h2>
                    <button type="button" className="btn btn-outline aq-add-tc-btn" onClick={addTestCase}>
                      + Adaugă test case
                    </button>
                  </div>
                  <p className="aq-hint">Fiecare test case validează automat codul candidatului.</p>

                  {testCases.map((tc, i) => (
                    <div key={i} className="aq-tc-block">
                      <div className="aq-tc-block-header">
                        <span className="aq-tc-num">Test case #{i + 1}</span>
                        {testCases.length > 1 && (
                          <button type="button" className="aq-tc-remove" onClick={() => removeTestCase(i)}>✕</button>
                        )}
                      </div>
                      <div className="aq-tc-fields">
                        <label className="aq-label">
                          Descriere *
                          <input
                            type="text"
                            className="aq-input"
                            placeholder="ex: Array cu elemente duplicate"
                            value={tc.description}
                            onChange={e => updateTestCase(i, "description", e.target.value)}
                          />
                        </label>
                        <label className="aq-label">
                          Input (stdin) — opțional
                          <textarea
                            className="aq-textarea aq-code"
                            rows={3}
                            placeholder="ex: 5&#10;1 2 3 4 5"
                            value={tc.inputData}
                            onChange={e => updateTestCase(i, "inputData", e.target.value)}
                          />
                        </label>
                        <label className="aq-label">
                          Output așteptat *
                          <textarea
                            className="aq-textarea aq-code"
                            rows={3}
                            placeholder="ex: 15"
                            value={tc.expectedOutput}
                            onChange={e => updateTestCase(i, "expectedOutput", e.target.value)}
                          />
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}

            {/* ── Submit ── */}
            <div className="aq-submit-row">
              <button type="button" className="btn btn-outline" onClick={resetForm}>
                Resetează
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSubmit}
                disabled={saving}
              >
                {saving ? "Se salvează..." : "Salvează întrebarea"}
              </button>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}
