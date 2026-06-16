import React, { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api";
import PageLayout from "../components/PageLayout";
// @ts-ignore
import "./InterviewResultPage.css";

type InterviewResult = {
  interviewId: number;
  status: string;
  totalMcq: number;
  correctMcq: number;
  score: number;
  readyLevel: string | null;
  summaryText: string | null;
  strongPoints: string | null;
  weakPoints: string | null;
  recommendedTopics: string | null;
  recommendedProblemCategories: string | null;
  nextSteps: string | null;
};

function readyLabel(level: string | null) {
  switch (level) {
    case "READY": return "Pregătit";
    case "PARTIALLY_READY": return "Parțial pregătit";
    case "NOT_READY": return "Nepregătit";
    default: return level ?? "—";
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

function scoreColor(score: number) {
  if (score >= 80) return "var(--clr-success)";
  if (score >= 50) return "var(--clr-warning)";
  return "var(--clr-danger)";
}

function normalizeFeedbackValue(value: unknown) {
  if (value === null || value === undefined) return "";
  return String(value).trim();
}

function isEmptyFeedback(value: unknown) {
  const normalized = normalizeFeedbackValue(value).toLowerCase();
  if (!normalized) return true;
  return ["n/a", "niciuna", "niciuna identificata", "-"].includes(normalized);
}

function splitFeedbackItems(value: unknown) {
  if (isEmptyFeedback(value)) return [];
  return normalizeFeedbackValue(value)
    .split(/\n|;|\s-\s|,\s(?=[A-ZA-Za-z0-9])/)
    .map((item) => item.trim().replace(/^[-*]\s*/, ""))
    .filter((item) => item.length > 1);
}

function FeedbackText({ value }: { value: unknown }) {
  const text = normalizeFeedbackValue(value);
  const items = splitFeedbackItems(text);
  if (items.length >= 2 && items.length <= 8) {
    return (
      <ul className="overview-list">
        {items.map((item, index) => <li key={`${item}-${index}`}>{item}</li>)}
      </ul>
    );
  }
  return <p>{text}</p>;
}

function TopicChips({ value }: { value: unknown }) {
  const items = splitFeedbackItems(value);
  if (items.length === 0) return null;
  return (
    <div className="overview-chip-list">
      {items.map((item, index) => (
        <span key={`${item}-${index}`} className="overview-chip">{item}</span>
      ))}
    </div>
  );
}

export default function InterviewResultPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();

  const stateResult = location.state as InterviewResult | undefined;
  const [result, setResult] = useState<InterviewResult | null>(stateResult ?? null);
  const [loading, setLoading] = useState(!stateResult);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id || stateResult) return;
    async function loadResult() {
      try {
        const res = await api.get<InterviewResult>(`/api/interview/${id}/result`);
        setResult(res.data);
      } catch (err: any) {
        setError(err?.response?.data?.message || "Nu am putut încărca rezultatul.");
      } finally {
        setLoading(false);
      }
    }
    loadResult();
  }, [id, stateResult]);

  if (loading) {
    return (
      <PageLayout>
        <div className="result-loading">Se calculează rezultatul...</div>
      </PageLayout>
    );
  }

  if (error || !result) {
    return (
      <PageLayout>
        <div className="result-error">
          <div className="error-box">{error || "Rezultatul nu este disponibil."}</div>
          <button className="btn btn-outline" onClick={() => navigate("/")}>
            Înapoi acasă
          </button>
        </div>
      </PageLayout>
    );
  }

  const scoreMsg =
    result.score >= 80
      ? "Rezultat excelent! Ai o bază tehnică solidă."
      : result.score >= 50
      ? "Rezultat bun, dar mai există loc de îmbunătățire."
      : "Mai exersează întrebările tehnice și refă un interviu.";

  return (
    <PageLayout>
      <div className="result-container">
        {/* Hero */}
        <div className="result-hero">
          <div className="result-hero-text">
            <h1>Interviu finalizat! 🎉</h1>
            <p>Interviu #{result.interviewId} • {result.status}</p>
          </div>
          {result.readyLevel && (
            <span className={`${readyBadgeClass(result.readyLevel)} result-ready-badge`}>
              {readyLabel(result.readyLevel)}
            </span>
          )}
        </div>

        {/* Score + stats */}
        <div className="result-stats">
          <div className="card result-score-card">
            <p className="result-stat-label">Scor general</p>
            <p
              className="result-score-value"
              style={{ color: scoreColor(result.score) }}
            >
              {result.score}%
            </p>
            <div className="result-score-bar-track">
              <div
                className="result-score-bar-fill"
                style={{
                  width: `${result.score}%`,
                  background: scoreColor(result.score),
                }}
              />
            </div>
          </div>

          <div className="card result-stat-card">
            <p className="result-stat-label">Grile corecte</p>
            <p className="result-stat-value">
              {result.correctMcq}
              <span className="result-stat-total">/ {result.totalMcq}</span>
            </p>
          </div>

          <div className="card result-stat-card">
            <p className="result-stat-label">Interpretare</p>
            <p className="result-stat-msg">{scoreMsg}</p>
          </div>
        </div>
        {/* AI Overview */}
        {result.readyLevel && (
          <div className="card result-overview feedback-summary-card">
            <div className="feedback-summary-header">
              <div>
                <span className="feedback-eyebrow">Diagnostic general</span>
                <h2>Feedback general AI</h2>
              </div>
              <div className="feedback-summary-badges">
                <span className="feedback-score-pill">{result.score}%</span>
                <span className={readyBadgeClass(result.readyLevel)}>
                  {readyLabel(result.readyLevel)}
                </span>
              </div>
            </div>

            {result.summaryText && (
              <div className="feedback-summary-callout">
                <span className="overview-label">Verdict</span>
                <p>{result.summaryText}</p>
              </div>
            )}

            <div className="feedback-action-grid">
              {result.strongPoints && (
                <div className="overview-section overview-good">
                  <span className="overview-label">Puncte forte</span>
                  <FeedbackText value={result.strongPoints} />
                </div>
              )}
              {result.weakPoints && (
                <div className="overview-section overview-bad">
                  <span className="overview-label">De imbunatatit</span>
                  <FeedbackText value={result.weakPoints} />
                </div>
              )}
              {result.nextSteps && (
                <div className="overview-section overview-next">
                  <span className="overview-label">Pasi urmatori</span>
                  <FeedbackText value={result.nextSteps} />
                </div>
              )}
            </div>

            {(result.recommendedTopics || result.recommendedProblemCategories) && (
              <div className="feedback-study-block">
                {result.recommendedTopics && (
                  <div className="overview-section">
                    <span className="overview-label">Capitole recomandate</span>
                    <TopicChips value={result.recommendedTopics} />
                  </div>
                )}
                {result.recommendedProblemCategories && (
                  <div className="overview-section">
                    <span className="overview-label">Categorii de probleme</span>
                    <TopicChips value={result.recommendedProblemCategories} />
                  </div>
                )}
              </div>
            )}
          </div>
        )}
        {/* Actions */}
        <div className="result-actions">
          <button className="btn btn-outline" onClick={() => navigate("/")}>
            Acasă
          </button>
          <button
            className="btn btn-outline"
            onClick={() => navigate(`/interview/${result.interviewId}/review`)}
          >
            Review complet
          </button>
          <button
            className="btn btn-primary"
            onClick={() => navigate("/interview/setup")}
          >
            Interviu nou →
          </button>
        </div>
      </div>
    </PageLayout>
  );
}
