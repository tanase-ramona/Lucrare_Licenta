import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/api";
import PageLayout from "../components/PageLayout";
// @ts-ignore
import "./InterviewSetupPage.css";

type FilterItem = { id: number; name: string };
type FiltersResponse = {
  levels: FilterItem[];
  positions: FilterItem[];
  languages: FilterItem[];
};

export default function InterviewSetupPage() {
  const navigate = useNavigate();

  const [levels, setLevels] = useState<FilterItem[]>([]);
  const [positions, setPositions] = useState<FilterItem[]>([]);
  const [languages, setLanguages] = useState<FilterItem[]>([]);

  const [levelId, setLevelId] = useState<number | "">("");
  const [positionId, setPositionId] = useState<number | "">("");
  const [selectedLanguageIds, setSelectedLanguageIds] = useState<number[]>([]);

  // Custom structure
  const [customizing, setCustomizing] = useState(false);
  const [hrCount, setHrCount] = useState(2);
  const [techCount, setTechCount] = useState(4);
  const [codingCount, setCodingCount] = useState(2);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadFilters() {
      try {
        const res = await api.get<FiltersResponse>("/api/interview/filters");
        setLevels(res.data.levels);
        setPositions(res.data.positions);
        setLanguages(res.data.languages);
      } catch {
        setError("Nu am putut încărca filtrele. Încearcă din nou.");
      } finally {
        setLoading(false);
      }
    }
    loadFilters();
  }, []);

  function toggleLanguage(id: number) {
    setSelectedLanguageIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  function clamp(val: number, min: number, max: number) {
    return Math.max(min, Math.min(max, val));
  }

  async function handleGenerate() {
    setError("");
    if (!levelId || !positionId || selectedLanguageIds.length === 0) {
      setError("Selectează nivelul, poziția și cel puțin un limbaj.");
      return;
    }
    if (customizing && hrCount + techCount + codingCount === 0) {
      setError("Structura personalizată trebuie să aibă cel puțin o întrebare.");
      return;
    }
    try {
      setSubmitting(true);
      const res = await api.post("/api/interview/generate", {
        levelId,
        positionId,
        languageIds: selectedLanguageIds,
        ...(customizing && { hrCount, techCount, codingCount }),
      });
      navigate(`/interview/${res.data.interviewId}`);
    } catch (err: any) {
      setError(err?.response?.data?.message || "Nu am putut genera interviul.");
    } finally {
      setSubmitting(false);
    }
  }

  const totalQuestions = customizing
    ? hrCount + techCount + codingCount
    : 2 + 4 + 2;

  if (loading) {
    return (
      <PageLayout>
        <div className="setup-loading">Se încarcă filtrele...</div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="setup-container">
        <div className="setup-layout">
          {/* Main config card */}
          <div className="card setup-card">
            <div className="setup-card-header">
              <h1 className="text-gradient">Configurează interviul</h1>
              <p>Alege nivelul, poziția și limbajele pentru care vrei să te antrenezi.</p>
            </div>

            {error && <div className="error-box">{error}</div>}

            <div className="setup-field">
              <label className="setup-label">Nivel de experiență</label>
              <select
                className="input setup-select"
                value={levelId}
                onChange={(e) => setLevelId(Number(e.target.value))}
              >
                <option value="">Selectează nivelul</option>
                {levels.map((l) => (
                  <option key={l.id} value={l.id}>{l.name}</option>
                ))}
              </select>
            </div>

            <div className="setup-field">
              <label className="setup-label">Poziție</label>
              <select
                className="input setup-select"
                value={positionId}
                onChange={(e) => setPositionId(Number(e.target.value))}
              >
                <option value="">Selectează poziția</option>
                {positions.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>

            <div className="setup-field">
              <label className="setup-label">
                Limbaje
                {selectedLanguageIds.length > 0 && (
                  <span className="setup-lang-count">
                    {selectedLanguageIds.length} selectate
                  </span>
                )}
              </label>
              <div className="lang-chips">
                {languages.map((lang) => {
                  const selected = selectedLanguageIds.includes(lang.id);
                  return (
                    <button
                      key={lang.id}
                      type="button"
                      className={`lang-chip ${selected ? "lang-chip-active" : ""}`}
                      onClick={() => toggleLanguage(lang.id)}
                    >
                      {lang.name}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Customizable structure toggle */}
            <div className="setup-field">
              <button
                type="button"
                className="setup-customize-toggle"
                onClick={() => setCustomizing((v) => !v)}
              >
                <span>{customizing ? "▼" : "▶"}</span>
                Personalizează structura interviului
                {!customizing && (
                  <span className="setup-default-badge">implicit: 2 HR · 4 tehnice · 2 codare</span>
                )}
              </button>

              {customizing && (
                <div className="setup-custom-grid">
                  <div className="setup-custom-field">
                    <label className="setup-label">Întrebări HR</label>
                    <input
                      type="number"
                      className="input"
                      min={0} max={10}
                      value={hrCount}
                      onChange={(e) => setHrCount(clamp(Number(e.target.value), 0, 10))}
                    />
                    <span className="setup-custom-hint">răspuns liber</span>
                  </div>
                  <div className="setup-custom-field">
                    <label className="setup-label">Întrebări tehnice</label>
                    <input
                      type="number"
                      className="input"
                      min={0} max={10}
                      value={techCount}
                      onChange={(e) => setTechCount(clamp(Number(e.target.value), 0, 10))}
                    />
                    <span className="setup-custom-hint">grilă</span>
                  </div>
                  <div className="setup-custom-field">
                    <label className="setup-label">Probleme de codare</label>
                    <input
                      type="number"
                      className="input"
                      min={0} max={10}
                      value={codingCount}
                      onChange={(e) => setCodingCount(clamp(Number(e.target.value), 0, 10))}
                    />
                    <span className="setup-custom-hint">cod</span>
                  </div>
                  <div className="setup-custom-total">
                    Total: <strong>{totalQuestions} întrebări</strong>
                  </div>
                </div>
              )}
            </div>

            <div className="setup-actions">
              <button className="btn btn-outline" onClick={() => navigate("/")}>
                Înapoi
              </button>
              <button
                className="btn btn-primary"
                onClick={handleGenerate}
                disabled={submitting}
              >
                {submitting ? "Se generează..." : "Generează interviu →"}
              </button>
            </div>
          </div>

        </div>
      </div>
    </PageLayout>
  );
}
