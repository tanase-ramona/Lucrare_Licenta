import React, { useEffect, useState } from "react";
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

  async function handleGenerate() {
    setError("");
    if (!levelId || !positionId || selectedLanguageIds.length === 0) {
      setError("Selectează nivelul, poziția și cel puțin un limbaj.");
      return;
    }
    try {
      setSubmitting(true);
      const res = await api.post("/api/interview/generate", {
        levelId,
        positionId,
        languageIds: selectedLanguageIds,
      });
      navigate(`/interview/${res.data.interviewId}`);
    } catch (err: any) {
      setError(err?.response?.data?.message || "Nu am putut genera interviul.");
    } finally {
      setSubmitting(false);
    }
  }

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

          {/* Sidebar info */}
          <div className="setup-sidebar">
            <div className="card setup-hint">
              <h3>Ce vei primi</h3>
              <ul className="hint-list">
                <li>
                  <span className="hint-icon">💬</span>
                  <div>
                    <strong>2 întrebări HR</strong>
                    <p>Răspuns liber — comportament, motivație</p>
                  </div>
                </li>
                <li>
                  <span className="hint-icon">🧩</span>
                  <div>
                    <strong>4 întrebări tehnice</strong>
                    <p>Grilă cu o singură variantă corectă</p>
                  </div>
                </li>
                <li>
                  <span className="hint-icon">💻</span>
                  <div>
                    <strong>2 probleme de codare</strong>
                    <p>Răspuns liber cu cod</p>
                  </div>
                </li>
              </ul>
            </div>

            <div className="card setup-tip">
              <span className="setup-tip-icon">💡</span>
              <p>
                Răspunde la toate întrebările pentru a obține cel mai bun
                feedback AI posibil.
              </p>
            </div>
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
