import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api";
import PageLayout from "../components/PageLayout";
// @ts-ignore
import "./InterviewReviewPage.css";

type OptionItem = { id: number; text: string; correct: boolean };

type HrFeedback = {
  score: number;
  good: boolean;
  matchedPoints: string;
  strongPoints: string;
  problematicMentions: string;
  missingPoints: string;
  improvementTips: string;
  idealAnswerStructure: string;
  tailoredExampleAnswer: string;
  toneFeedback: string;
  finalVerdict: string;
};

type CodingFeedback = {
  score: number;
  good: boolean;
  problemUnderstanding: string;
  correctness: string;
  codeRuns: string;
  runIssues: string;
  logicIssues: string;
  syntaxIssues: string;
  edgeCases: string;
  timeComplexity: string;
  spaceComplexity: string;
  goodPracticesUsed: string;
  badPracticesUsed: string;
  testedExamples: string;
  strengths: string;
  weaknesses: string;
  improvementTips: string;
  correctedCode: string;
  modelSolution: string;
  explanationOfSolution: string;
  finalVerdict: string;
};

type QuestionReviewItem = {
  interviewQuestionId: number;
  text: string;
  category: string;
  answerType: string;
  options: OptionItem[];
  languages: string[];
  answerText: string | null;
  selectedOptionId: number | null;
  aiScore: number | null;
  aiGood: boolean | null;
  aiStrengths: string | null;
  aiWeaknesses: string | null;
  aiImprovementTips: string | null;
  aiSuggestedAnswer: string | null;
  aiFeedbackJson: string | null;
};

type FeedbackSummary = {
  readyLevel: string | null;
  summaryText: string | null;
  strongPoints: string | null;
  weakPoints: string | null;
  recommendedTopics: string | null;
  recommendedProblemCategories: string | null;
  nextSteps: string | null;
};

type InterviewReviewResponse = {
  interviewId: number;
  status: string;
  score: number | null;
  feedbackSummary: FeedbackSummary | null;
  questions: QuestionReviewItem[];
};

function parseJson<T>(json: string | null): T | null {
  if (!json) return null;
  try { return JSON.parse(json) as T; } catch { return null; }
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
      <ul className="fb-row-list">
        {items.map((item, index) => <li key={`${item}-${index}`}>{item}</li>)}
      </ul>
    );
  }
  return <p className="fb-row-value">{text}</p>;
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

function FeedbackRow({ label, value, variant }: { label: string; value: unknown; variant?: "good" | "bad" | "neutral" | "code" }) {
  if (isEmptyFeedback(value)) return null;
  const text = normalizeFeedbackValue(value);
  return (
    <div className={`fb-row fb-row-${variant ?? "neutral"}`}>
      <span className="fb-row-label">{label}</span>
      {variant === "code"
        ? <pre className="fb-code-block"><code>{text}</code></pre>
        : <FeedbackText value={text} />
      }
    </div>
  );
}

function HrFeedbackPanel({ fb }: { fb: HrFeedback }) {
  return (
    <div className="fb-panel">
      <div className="fb-panel-header">
        <span className="fb-panel-title">Feedback AI — Răspuns liber {fb.good ? "✅" : "⚠️"}</span>
        <span className="fb-score">{fb.score}/10</span>
      </div>

      <div className="fb-verdict">{fb.finalVerdict}</div>

      <div className="fb-grid-2">
        <FeedbackRow label="Ce a atins bine" value={fb.matchedPoints} variant="good" />
        <FeedbackRow label="Puncte forte" value={fb.strongPoints} variant="good" />
        <FeedbackRow label="Ce lipsește" value={fb.missingPoints} variant="bad" />
        <FeedbackRow label="Formulări problematice" value={fb.problematicMentions} variant="bad" />
      </div>

      <FeedbackRow label="💡 Sfaturi de îmbunătățire" value={fb.improvementTips} />
      <FeedbackRow label="🎯 Structura unui răspuns ideal" value={fb.idealAnswerStructure} />
      <FeedbackRow label="🗣 Ton și claritate" value={fb.toneFeedback} />

      {fb.tailoredExampleAnswer && (
        <div className="fb-row fb-row-example">
          <span className="fb-row-label">📝 Exemplu de răspuns adaptat</span>
          <p className="fb-row-value fb-example-text">{fb.tailoredExampleAnswer}</p>
        </div>
      )}
    </div>
  );
}

function CodingFeedbackPanel({ fb }: { fb: CodingFeedback }) {
  const runsColor = fb.codeRuns === "true" ? "good" : fb.codeRuns === "false" ? "bad" : "neutral";

  return (
    <div className="fb-panel">
      <div className="fb-panel-header">
        <span className="fb-panel-title">Feedback AI — Coding {fb.good ? "✅" : "⚠️"}</span>
        <span className="fb-score">{fb.score}/10</span>
      </div>

      <div className="fb-verdict">{fb.finalVerdict}</div>

      <div className="fb-inline-badges">
        <div className={`fb-badge fb-badge-${runsColor}`}>
          Rulează: {fb.codeRuns === "true" ? "Da" : fb.codeRuns === "false" ? "Nu" : "Neclar"}
        </div>
        {fb.timeComplexity && <div className="fb-badge fb-badge-neutral">Timp: {fb.timeComplexity}</div>}
        {fb.spaceComplexity && <div className="fb-badge fb-badge-neutral">Spațiu: {fb.spaceComplexity}</div>}
      </div>

      <div className="fb-grid-2">
        <FeedbackRow label="Înțelegerea problemei" value={fb.problemUnderstanding} variant="neutral" />
        <FeedbackRow label="Corectitudine" value={fb.correctness} variant="neutral" />
        <FeedbackRow label="Puncte forte" value={fb.strengths} variant="good" />
        <FeedbackRow label="Puncte slabe" value={fb.weaknesses} variant="bad" />
        <FeedbackRow label="Probleme de logică" value={fb.logicIssues} variant="bad" />
        <FeedbackRow label="Probleme de sintaxă" value={fb.syntaxIssues} variant="bad" />
        <FeedbackRow label="De ce nu rulează" value={fb.runIssues} variant="bad" />
        <FeedbackRow label="Edge cases netratate" value={fb.edgeCases} variant="bad" />
        <FeedbackRow label="Bune practici" value={fb.goodPracticesUsed} variant="good" />
        <FeedbackRow label="Practici proaste" value={fb.badPracticesUsed} variant="bad" />
      </div>

      <FeedbackRow label="🧪 Exemple testate" value={fb.testedExamples} />
      <FeedbackRow label="💡 Sfaturi de îmbunătățire" value={fb.improvementTips} />

      {fb.correctedCode && fb.correctedCode !== "N/A" && (
        <div className="fb-row fb-row-code-section">
          <span className="fb-row-label">🔧 Codul tău corectat</span>
          <pre className="fb-code-block"><code>{fb.correctedCode}</code></pre>
        </div>
      )}

      {fb.modelSolution && (
        <div className="fb-row fb-row-code-section">
          <span className="fb-row-label">✨ Soluție model</span>
          <pre className="fb-code-block"><code>{fb.modelSolution}</code></pre>
          {fb.explanationOfSolution && (
            <p className="fb-explanation">{fb.explanationOfSolution}</p>
          )}
        </div>
      )}
    </div>
  );
}

function AiFeedbackSection({ q }: { q: QuestionReviewItem }) {
  if (q.aiScore === null) return null;

  const hrFb = q.answerType === "TEXT" ? parseJson<HrFeedback>(q.aiFeedbackJson) : null;
  const codingFb = q.answerType === "CODE" ? parseJson<CodingFeedback>(q.aiFeedbackJson) : null;

  if (hrFb) return <HrFeedbackPanel fb={hrFb} />;
  if (codingFb) return <CodingFeedbackPanel fb={codingFb} />;

  // fallback — daca feedbackJson lipseste, afisam feedbackul vechi simplu
  return (
    <div className="fb-panel">
      <div className="fb-panel-header">
        <span className="fb-panel-title">Feedback AI {q.aiGood ? "✅" : "⚠️"}</span>
        <span className="fb-score">{q.aiScore}/10</span>
      </div>
      <div className="fb-grid-2">
        {q.aiStrengths && (
          <div className="fb-row fb-row-good">
            <span className="fb-row-label">Puncte bune</span>
            <p className="fb-row-value">{q.aiStrengths}</p>
          </div>
        )}
        {q.aiWeaknesses && (
          <div className="fb-row fb-row-bad">
            <span className="fb-row-label">Ce lipsește</span>
            <p className="fb-row-value">{q.aiWeaknesses}</p>
          </div>
        )}
      </div>
      {q.aiImprovementTips && (
        <div className="fb-row">
          <span className="fb-row-label">💡 Îmbunătățiri</span>
          <p className="fb-row-value">{q.aiImprovementTips}</p>
        </div>
      )}
      {q.aiSuggestedAnswer && (
        <div className="fb-row">
          <span className="fb-row-label">📝 Răspuns sugerat</span>
          <p className="fb-row-value">{q.aiSuggestedAnswer}</p>
        </div>
      )}
    </div>
  );
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

function FeedbackSummaryCard({ summary, score }: { summary: FeedbackSummary; score: number | null }) {
  return (
    <div className="card result-overview feedback-summary-card">
      <div className="feedback-summary-header">
        <div>
          <span className="feedback-eyebrow">Diagnostic general</span>
          <h2>Feedback general AI</h2>
        </div>
        <div className="feedback-summary-badges">
          {score !== null && <span className="feedback-score-pill">{score}%</span>}
          {summary.readyLevel && (
            <span className={readyBadgeClass(summary.readyLevel)}>
              {readyBadgeLabel(summary.readyLevel)}
            </span>
          )}
        </div>
      </div>

      {summary.summaryText && (
        <div className="feedback-summary-callout">
          <span className="overview-label">Verdict</span>
          <p>{summary.summaryText}</p>
        </div>
      )}

      <div className="feedback-action-grid">
        {summary.strongPoints && (
          <div className="overview-section overview-good">
            <span className="overview-label">Puncte forte</span>
            <FeedbackText value={summary.strongPoints} />
          </div>
        )}
        {summary.weakPoints && (
          <div className="overview-section overview-bad">
            <span className="overview-label">De imbunatatit</span>
            <FeedbackText value={summary.weakPoints} />
          </div>
        )}
        {summary.nextSteps && (
          <div className="overview-section overview-next">
            <span className="overview-label">Pasi urmatori</span>
            <FeedbackText value={summary.nextSteps} />
          </div>
        )}
      </div>

      {(summary.recommendedTopics || summary.recommendedProblemCategories) && (
        <div className="feedback-study-block">
          {summary.recommendedTopics && (
            <div className="overview-section">
              <span className="overview-label">Capitole recomandate</span>
              <TopicChips value={summary.recommendedTopics} />
            </div>
          )}
          {summary.recommendedProblemCategories && (
            <div className="overview-section">
              <span className="overview-label">Categorii de probleme</span>
              <TopicChips value={summary.recommendedProblemCategories} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function InterviewReviewPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [review, setReview] = useState<InterviewReviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadReview() {
      try {
        const res = await api.get<InterviewReviewResponse>(`/api/interview/${id}/review`);
        setReview(res.data);
      } catch (err: any) {
        setError(err?.response?.data?.message || "Nu am putut încărca detaliile interviului.");
      } finally {
        setLoading(false);
      }
    }
    if (id) loadReview();
  }, [id]);

  if (loading) return (
    <PageLayout>
      <div className="review-loading">Se încarcă detaliile interviului...</div>
    </PageLayout>
  );

  if (error) return (
    <PageLayout>
      <div className="review-error">
        <div className="error-box">{error}</div>
        <button className="btn btn-outline" onClick={() => navigate("/")}>Înapoi acasă</button>
      </div>
    </PageLayout>
  );

  if (!review) return null;

  return (
    <PageLayout>
      <div className="review-container">
        {/* Header */}
        <div className="card review-header-card">
          <div>
            <h1 className="text-gradient">Review interviu #{review.interviewId}</h1>
            <p>
              {review.score !== null
                ? `Scor final: ${review.score}% • ${review.status}`
                : review.status}
            </p>
          </div>
          <div className="review-header-actions">
            <button className="btn btn-outline" onClick={() => navigate("/")}>Acasă</button>
            <button className="btn btn-primary" onClick={() => navigate("/interview/setup")}>Interviu nou</button>
          </div>
        </div>

        {/* Feedback general */}
        {review.feedbackSummary && (
          <FeedbackSummaryCard summary={review.feedbackSummary} score={review.score} />
        )}
        {/* Questions */}
        <div className="review-question-list">
          {review.questions.map((q, index) => (
            <details className="card review-question-card" key={q.interviewQuestionId} open={index === 0}>
              <summary className="review-question-summary">
                <div className="review-question-summary-main">
                  <div className="review-question-meta">
                    <span className="badge badge-gray">#{index + 1}</span>
                    <span className={`badge ${q.category === "HR" ? "badge-blue" : q.category === "TECH" ? "badge-purple" : "badge-warning"}`}>
                      {q.category}
                    </span>
                    <span className="badge badge-gray">{q.answerType}</span>
                    {q.languages.map((lang) => (
                      <span key={lang} className="badge badge-blue">{lang}</span>
                    ))}
                  </div>
                  <h3 className="review-question-text">{q.text}</h3>
                </div>
                {q.aiScore !== null && <span className="question-score-pill">{q.aiScore}/10</span>}
              </summary>

              <div className="review-question-content">
                {q.answerType === "MCQ" ? (
                  <div className="review-mcq-options">
                    {q.options.map((option) => {
                      const selected = q.selectedOptionId === option.id;
                      const correct = option.correct;
                      let cls = "review-mcq-option";
                      if (correct) cls += " review-mcq-correct";
                      if (selected && !correct) cls += " review-mcq-wrong";
                      return (
                        <div key={option.id} className={cls}>
                          <span className="review-mcq-marker">
                            {selected && correct ? "OK" : selected && !correct ? "X" : correct ? "OK" : "o"}
                          </span>
                          <span>{option.text}</span>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <>
                    <div className="review-section-label">Raspunsul tau</div>
                    <div className={`review-answer-box ${q.answerType === "CODE" ? "review-answer-code" : ""}`}>
                      {q.answerText?.trim() || "Niciun raspuns salvat pentru aceasta intrebare."}
                    </div>
                    <AiFeedbackSection q={q} />
                  </>
                )}
              </div>
            </details>
          ))}
        </div>
        <div className="review-bottom-actions">
          <button className="btn btn-outline" onClick={() => navigate("/")}>Acasă</button>
          <button className="btn btn-primary" onClick={() => navigate("/interview/setup")}>Interviu nou →</button>
        </div>
      </div>
    </PageLayout>
  );
}
