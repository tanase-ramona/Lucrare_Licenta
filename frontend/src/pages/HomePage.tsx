import React, { useEffect, useMemo, useState } from "react";
import { api } from "../api/api";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import PageLayout from "../components/PageLayout";
// @ts-ignore
import "./HomePage.css";

type InterviewHistoryItem = {
  interviewId: number;
  status: string;
  score: number | null;
  createdAt: string;
  level: string;
  position: string;
  readyLevel: string | null;
};

export default function HomePage() {
  const navigate = useNavigate();
  const { state } = useAuth();

  const [interviews, setInterviews] = useState<InterviewHistoryItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);

  const email = state.user?.email ?? "user";
  const firstName = useMemo(() => {
    const left = email.split("@")[0] ?? "user";
    return left.charAt(0).toUpperCase() + left.slice(1);
  }, [email]);

  const stats = useMemo(() => {
    const completed = interviews.filter((i) => i.status === "COMPLETED");
    const scored = completed.filter((i) => i.score !== null);
    const avgScore =
      scored.length > 0
        ? Math.round(
            scored.reduce((sum, i) => sum + (i.score ?? 0), 0) / scored.length
          )
        : null;
    return {
      total: interviews.length,
      completed: completed.length,
      avgScore,
    };
  }, [interviews]);

  useEffect(() => {
    async function loadHistory() {
      try {
        setHistoryLoading(true);
        const res = await api.get<InterviewHistoryItem[]>("/api/interview/history");
        setInterviews(res.data);
      } catch {
        // silently fail — table will stay empty
      } finally {
        setHistoryLoading(false);
      }
    }
    loadHistory();
  }, []);

  async function handleDelete(interviewId: number) {
    if (!window.confirm("Ești sigur că vrei să ștergi acest interviu?")) return;
    try {
      await api.delete(`/api/interview/${interviewId}`);
      setInterviews((prev) => prev.filter((i) => i.interviewId !== interviewId));
    } catch {
      alert("Nu s-a putut șterge interviul. Încearcă din nou.");
    }
  }

  function readyBadgeClass(level: string | null) {
    switch (level) {
      case "READY": return "badge badge-success";
      case "PARTIALLY_READY": return "badge badge-warning";
      case "NOT_READY": return "badge badge-danger";
      default: return "badge badge-gray";
    }
  }

  function readyBadgeLabel(level: string | null) {
    switch (level) {
      case "READY": return "Pregătit";
      case "PARTIALLY_READY": return "Parțial pregătit";
      case "NOT_READY": return "Nepregătit";
      default: return "—";
    }
  }

  return (
    <PageLayout>
      <div className="home-container">
        {/* Hero */}
        <div className="home-hero">
          <div className="home-hero-text">
            <h2>Bun venit, {firstName}!</h2>
            <p>Ești gata pentru un nou interviu? Configurează și pornește chiar acum.</p>
          </div>
          <div style={{ display: "flex", gap: "12px", flexShrink: 0 }}>
            <button
              className="btn btn-outline"
              style={{ background: "rgba(255,255,255,0.12)", borderColor: "rgba(255,255,255,0.25)", color: "white" }}
              onClick={() => navigate("/recommendations")}
            >
              📚 Recomandări
            </button>
            <button
              className="home-hero-btn"
              onClick={() => navigate("/interview/setup")}
            >
              ▶ Interviu nou
            </button>
          </div>
        </div>

        {/* Stats */}
        <div className="stats-row">
          <div className="card stat-card">
            <span className="stat-icon">🗂️</span>
            <div>
              <p className="stat-label">Total interviuri</p>
              <p className="stat-value">{stats.total}</p>
            </div>
          </div>

          <div className="card stat-card">
            <span className="stat-icon">✅</span>
            <div>
              <p className="stat-label">Finalizate</p>
              <p className="stat-value">{stats.completed}</p>
            </div>
          </div>

          <div className="card stat-card">
            <span className="stat-icon">🏅</span>
            <div>
              <p className="stat-label">Scor mediu</p>
              <p className="stat-value">
                {stats.avgScore !== null ? `${stats.avgScore}%` : "—"}
              </p>
            </div>
          </div>

          <div className="card stat-card">
            <span className="stat-icon">📈</span>
            <div>
              <p className="stat-label">Cel mai bun scor</p>
              <p className="stat-value">
                {interviews.filter(i => i.score !== null).length > 0
                  ? `${Math.max(...interviews.filter(i => i.score !== null).map(i => i.score!))}%`
                  : "—"}
              </p>
            </div>
          </div>
        </div>

        {/* Info grid */}
        <div className="info-grid">
          <div className="card info-card">
            <h3 className="info-card-title">Cum funcționează</h3>
            <p className="info-card-subtitle">
              Trei pași simpli pentru un interviu complet.
            </p>
            <ul className="info-list">
              <li>Alege nivelul, poziția și limbajele dorite</li>
              <li>Răspunde la întrebări HR, tehnice și de codare</li>
              <li>Primește feedback AI și scor detaliat</li>
            </ul>
          </div>

          <div className="card info-card">
            <h3 className="info-card-title">Recomandări</h3>
            <p className="info-card-subtitle">
              Maximizează eficiența sesiunilor de pregătire.
            </p>
            <ul className="info-list">
              <li>Răspunde complet la toate întrebările înainte de finalizare</li>
              <li>Refă interviuri pentru roluri diferite și compară scorurile</li>
              <li>Folosește istoricul ca să urmărești progresul în timp</li>
            </ul>
          </div>
        </div>

        {/* History */}
        <div className="card history-card">
          <div className="history-header">
            <div>
              <h3 className="history-title">Interviuri recente</h3>
              <p className="history-subtitle">Istoricul tuturor interviurilor generate</p>
            </div>
            <button
              className="btn btn-primary"
              onClick={() => navigate("/interview/setup")}
            >
              + Interviu nou
            </button>
          </div>

          {historyLoading ? (
            <div className="history-empty">
              <p>Se încarcă istoricul...</p>
            </div>
          ) : interviews.length === 0 ? (
            <div className="history-empty">
              <p>Nu ai finalizat încă niciun interviu.</p>
              <button
                className="btn btn-outline"
                onClick={() => navigate("/interview/setup")}
              >
                Începe primul interviu
              </button>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Poziție</th>
                    <th>Nivel</th>
                    <th>Status</th>
                    <th>Scor</th>
                    <th>Pregătire</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {interviews.map((i) => (
                    <tr key={i.interviewId}>
                      <td>{new Date(i.createdAt).toLocaleDateString("ro-RO")}</td>
                      <td>{i.position}</td>
                      <td>{i.level}</td>
                      <td>
                        <span className={i.status === "COMPLETED" ? "badge badge-success" : "badge badge-gray"}>
                          {i.status === "COMPLETED" ? "Finalizat" : i.status}
                        </span>
                      </td>
                      <td>{i.score !== null ? `${i.score}%` : "—"}</td>
                      <td>
                        <span className={readyBadgeClass(i.readyLevel)}>
                          {readyBadgeLabel(i.readyLevel)}
                        </span>
                      </td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="btn btn-outline table-btn"
                            onClick={() => navigate(`/interview/${i.interviewId}/review`)}
                          >
                            Detalii
                          </button>
                          <button
                            className="btn btn-delete table-btn"
                            onClick={() => handleDelete(i.interviewId)}
                            title="Șterge interviul"
                          >
                            🗑
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </PageLayout>
  );
}
