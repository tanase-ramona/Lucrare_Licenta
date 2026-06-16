import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api";
import PageLayout from "../components/PageLayout";
import Editor from "@monaco-editor/react";
// @ts-ignore
import "./InterviewPage.css";

const LANG_MAP: Record<string, string> = {
  java: "java", python: "python", javascript: "javascript",
  typescript: "typescript", c: "c", "c++": "cpp", "c#": "csharp",
  sql: "sql", go: "go", kotlin: "kotlin", swift: "swift",
  react: "javascript", spring: "java", php: "php", ruby: "ruby",
};

const PISTON_LANG_MAP: Record<string, string> = {
  java: "java", python: "python", javascript: "javascript",
  typescript: "typescript", c: "c", cpp: "c++", csharp: "csharp",
  go: "go", kotlin: "kotlin", swift: "swift", php: "php", ruby: "ruby",
};

function detectMonacoLang(languages: string[]): string {
  for (const lang of languages) {
    const key = lang.toLowerCase();
    if (LANG_MAP[key]) return LANG_MAP[key];
  }
  return "plaintext";
}

function toPistonLang(monacoLang: string): string {
  return PISTON_LANG_MAP[monacoLang] ?? monacoLang;
}

type OptionItem = { id: number; text: string };

type QuestionItem = {
  interviewQuestionId: number;
  questionId: number;
  text: string;
  category: string;
  answerType: string;
  starterCode: string | null;
  options: OptionItem[];
  languages: string[];
};

type InterviewResponse = {
  id: number;
  status: string;
  questions: QuestionItem[];
};

type TestCase = {
  id: number;
  description: string;
  inputData: string | null;
  expectedOutput: string;
  orderIndex: number;
};

type TestCaseResult = {
  testCaseId: number;
  description: string;
  inputData: string | null;
  expectedOutput: string;
  actualOutput: string;
  passed: boolean;
  error: string | null;
  executionTimeMs: number;
};

type RunResult = {
  results: TestCaseResult[];
  passed: number;
  total: number;
};

// ─── Run Panel component ──────────────────────────────────────────────────────

function RunPanel({
  testCases,
  runResult,
  running,
  onRun,
}: {
  testCases: TestCase[];
  runResult: RunResult | null;
  running: boolean;
  onRun: () => void;
}) {
  const [expandedId, setExpandedId] = useState<number | null>(
    testCases[0]?.id ?? null
  );

  if (testCases.length === 0) return null;

  const allPassed  = runResult && runResult.passed === runResult.total;
  const somePassed = runResult && runResult.passed > 0 && !allPassed;

  const pct = runResult ? Math.round((runResult.passed / runResult.total) * 100) : 0;

  return (
    <div className="run-panel">
      {/* ── Terminal header ── */}
      <div className="run-terminal-header">
        <div className="run-terminal-dots">
          <span className="run-dot run-dot-red" />
          <span className="run-dot run-dot-yellow" />
          <span className="run-dot run-dot-green" />
        </div>
        <span className="run-terminal-title">test runner</span>

        <div className="run-terminal-right">
          {runResult && (
            <span className={`run-badge ${allPassed ? "run-badge-ok" : somePassed ? "run-badge-partial" : "run-badge-fail"}`}>
              {allPassed ? "✓" : somePassed ? "~" : "✗"} {runResult.passed}/{runResult.total} passed
            </span>
          )}
          <button
            className={`run-btn ${running ? "run-btn-loading" : ""}`}
            onClick={onRun}
            disabled={running}
          >
            {running
              ? <><span className="run-spinner" /> running...</>
              : <><span className="run-play">▶</span> Run Tests</>
            }
          </button>
        </div>
      </div>

      {/* ── Progress bar ── */}
      {runResult && (
        <div className="run-progress-track">
          <div
            className={`run-progress-fill ${allPassed ? "run-fill-ok" : somePassed ? "run-fill-partial" : "run-fill-fail"}`}
            style={{ width: `${pct}%` }}
          />
        </div>
      )}

      {/* ── Test cases ── */}
      <div className="run-cases">
        {testCases.map((tc, idx) => {
          const result  = runResult?.results.find(r => r.testCaseId === tc.id);
          const status  = !result ? "pending" : result.passed ? "passed" : "failed";
          const open    = expandedId === tc.id;

          return (
            <div key={tc.id} className={`run-case run-case-${status}`}>
              {/* Case header — click to expand */}
              <button
                className="run-case-header"
                onClick={() => setExpandedId(open ? null : tc.id)}
              >
                <div className="run-case-left">
                  <span className={`run-status-dot run-status-${status}`} />
                  <span className="run-case-num">Test {idx + 1}</span>
                  <span className="run-case-desc">{tc.description}</span>
                </div>
                <div className="run-case-right">
                  {result && (
                    <span className="run-exec-time">{result.executionTimeMs}ms</span>
                  )}
                  {status !== "pending" && (
                    <span className={`run-status-label run-status-label-${status}`}>
                      {status === "passed" ? "PASSED" : "FAILED"}
                    </span>
                  )}
                  <span className="run-chevron">{open ? "▲" : "▼"}</span>
                </div>
              </button>

              {/* Case body */}
              {open && (
                <div className="run-case-body">
                  {/* Input */}
                  {tc.inputData && (
                    <div className="run-io-block">
                      <div className="run-io-label">
                        <span className="run-io-icon">→</span> stdin
                      </div>
                      <pre className="run-io-code">{tc.inputData}</pre>
                    </div>
                  )}

                  {/* Expected vs Actual side by side */}
                  <div className={`run-io-compare ${result ? "run-io-compare-2col" : ""}`}>
                    <div className="run-io-block">
                      <div className="run-io-label">
                        <span className="run-io-icon">◎</span> expected output
                      </div>
                      <pre className="run-io-code">{tc.expectedOutput}</pre>
                    </div>

                    {result && !result.error && (
                      <div className="run-io-block">
                        <div className={`run-io-label ${result.passed ? "run-io-label-ok" : "run-io-label-fail"}`}>
                          <span className="run-io-icon">{result.passed ? "✓" : "✗"}</span>
                          {result.passed ? "your output" : "your output"}
                        </div>
                        <pre className={`run-io-code ${result.passed ? "run-io-code-ok" : "run-io-code-fail"}`}>
                          {result.actualOutput || "(no output)"}
                        </pre>
                      </div>
                    )}
                  </div>

                  {/* Error */}
                  {result?.error && (
                    <div className="run-io-block">
                      <div className="run-io-label run-io-label-fail">
                        <span className="run-io-icon">!</span> error
                      </div>
                      <pre className="run-io-code run-io-code-fail">{result.error}</pre>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function InterviewPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [interview, setInterview] = useState<InterviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [finishing, setFinishing] = useState(false);
  const [error, setError] = useState("");

  const [textAnswers, setTextAnswers] = useState<Record<number, string>>({});
  const [selectedOptions, setSelectedOptions] = useState<Record<number, number>>({});
  const [testCases, setTestCases] = useState<Record<number, TestCase[]>>({});
  const [runResults, setRunResults] = useState<Record<number, RunResult>>({});
  const [runningFor, setRunningFor] = useState<number | null>(null);

  useEffect(() => {
    async function loadInterview() {
      try {
        const res = await api.get<InterviewResponse>(`/api/interview/${id}`);
        setInterview(res.data);

        const starterAnswers: Record<number, string> = {};
        res.data.questions
          .filter((q) => q.answerType === "CODE" && q.starterCode)
          .forEach((q) => {
            starterAnswers[q.interviewQuestionId] = q.starterCode ?? "";
          });
        setTextAnswers(starterAnswers);

        // fetch test cases for all CODE questions in parallel
        const codeQuestions = res.data.questions.filter((q) => q.answerType === "CODE");
        const fetched: Record<number, TestCase[]> = {};
        await Promise.all(
          codeQuestions.map(async (q) => {
            try {
              const tc = await api.get<TestCase[]>(`/api/questions/${q.questionId}/testcases`);
              if (tc.data.length > 0) fetched[q.interviewQuestionId] = tc.data;
            } catch {
              // no test cases for this question — ok
            }
          })
        );
        setTestCases(fetched);
      } catch (err: any) {
        setError(err?.response?.data?.message || "Nu am putut încărca interviul.");
      } finally {
        setLoading(false);
      }
    }
    if (id) loadInterview();
  }, [id]);

  async function handleRunCode(q: QuestionItem) {
    const code = textAnswers[q.interviewQuestionId]?.trim();
    if (!code) {
      alert("Scrie mai întâi codul înainte de a rula testele.");
      return;
    }
    setRunningFor(q.interviewQuestionId);
    try {
      const monacoLang = detectMonacoLang(q.languages);
      const pistonLang = toPistonLang(monacoLang);
      const res = await api.post<RunResult>("/api/code/run", {
        language: pistonLang,
        code,
        questionId: q.questionId,
      });
      setRunResults((prev) => ({ ...prev, [q.interviewQuestionId]: res.data }));
    } catch {
      alert("Nu s-au putut rula testele. Încearcă din nou.");
    } finally {
      setRunningFor(null);
    }
  }

  function handleTextAnswer(questionId: number, value: string) {
    setError("");
    setTextAnswers((prev) => ({ ...prev, [questionId]: value }));
  }

  function handleOptionSelect(questionId: number, optionId: number) {
    setError("");
    setSelectedOptions((prev) => ({ ...prev, [questionId]: optionId }));
  }

  function handleResetStarterCode(q: QuestionItem) {
    if (!q.starterCode) return;
    setError("");
    setTextAnswers((prev) => ({
      ...prev,
      [q.interviewQuestionId]: q.starterCode ?? "",
    }));
    setRunResults((prev) => {
      const next = { ...prev };
      delete next[q.interviewQuestionId];
      return next;
    });
  }

  function validateAnswers(): string | null {
    if (!interview) return "Interviul nu este încărcat.";
    for (let i = 0; i < interview.questions.length; i++) {
      const q = interview.questions[i];
      if (q.answerType === "MCQ") {
        if (!selectedOptions[q.interviewQuestionId])
          return `Nu ai selectat un răspuns pentru întrebarea ${i + 1}.`;
      } else if (q.answerType === "CODE") {
        const answer = textAnswers[q.interviewQuestionId]?.trim() ?? "";
        const starter = q.starterCode?.trim() ?? "";
        if (!answer || (starter && answer === starter))
          return `Completează codul pentru întrebarea ${i + 1}.`;
      } else {
        if (!textAnswers[q.interviewQuestionId]?.trim())
          return `Nu ai completat răspunsul pentru întrebarea ${i + 1}.`;
      }
    }
    return null;
  }

  function answeredCount(): number {
    if (!interview) return 0;
    return interview.questions.filter((q) => {
      if (q.answerType === "MCQ") return !!selectedOptions[q.interviewQuestionId];
      if (q.answerType === "CODE") {
        const answer = textAnswers[q.interviewQuestionId]?.trim() ?? "";
        const starter = q.starterCode?.trim() ?? "";
        return !!answer && (!starter || answer !== starter);
      }
      return !!textAnswers[q.interviewQuestionId]?.trim();
    }).length;
  }

  async function handleFinish() {
    if (!interview) return;
    const validationError = validateAnswers();
    if (validationError) { setError(validationError); return; }

    setFinishing(true);
    try {
      const answersPayload = {
        answers: interview.questions.map((q) => {
          if (q.answerType === "MCQ") {
            return {
              interviewQuestionId: q.interviewQuestionId,
              selectedOptionId: selectedOptions[q.interviewQuestionId] || null,
            };
          }
          return {
            interviewQuestionId: q.interviewQuestionId,
            answerText: textAnswers[q.interviewQuestionId] || "",
          };
        }),
      };
      await api.post(`/api/interview/${interview.id}/answers`, answersPayload);
      const finishRes = await api.post(`/api/interview/${interview.id}/finish`);
      navigate(`/interview/${interview.id}/result`, { state: finishRes.data });
    } catch (err: any) {
      setFinishing(false);
      setError(err?.response?.data?.message || "Nu am putut finaliza interviul.");
    }
  }

  if (loading) return (
    <PageLayout>
      <div className="interview-loading">Se încarcă interviul...</div>
    </PageLayout>
  );

  if (error && !interview) return (
    <PageLayout>
      <div className="interview-error-page">
        <div className="error-box">{error}</div>
        <button className="btn btn-outline" onClick={() => navigate("/")}>Înapoi acasă</button>
      </div>
    </PageLayout>
  );

  if (!interview) return null;

  const total = interview.questions.length;
  const answered = answeredCount();
  const progressPct = total > 0 ? Math.round((answered / total) * 100) : 0;

  return (
    <PageLayout>
      <div className="interview-container">
        {/* Progress header */}
        <div className="card interview-progress-card">
          <div className="interview-progress-info">
            <div>
              <h1>Interviu #{interview.id}</h1>
              <p>{answered} din {total} întrebări completate</p>
            </div>
            <div className="interview-progress-pct">{progressPct}%</div>
          </div>
          <div className="progress-bar-track">
            <div className="progress-bar-fill" style={{ width: `${progressPct}%` }} />
          </div>
        </div>

        {error && <div className="error-box">{error}</div>}

        <div className="question-list">
          {interview.questions.map((q, index) => (
            <div className="card question-card" key={q.interviewQuestionId}>
              <div className="question-meta">
                <span className="badge badge-gray">#{index + 1}</span>
                <span className={`badge ${q.category === "HR" ? "badge-blue" : "badge-purple"}`}>
                  {q.category}
                </span>
                <span className="badge badge-gray">{q.answerType}</span>
                {q.languages.map((lang) => (
                  <span key={lang} className="badge badge-blue">{lang}</span>
                ))}
              </div>

              <h3 className="question-text">{q.text}</h3>

              {q.answerType === "TEXT" && (
                <textarea
                  className="answer-textarea"
                  rows={6}
                  placeholder="Scrie răspunsul tău aici..."
                  value={textAnswers[q.interviewQuestionId] || ""}
                  onChange={(e) => handleTextAnswer(q.interviewQuestionId, e.target.value)}
                />
              )}

              {q.answerType === "CODE" && (
                <>
                  <div className="code-editor-wrapper">
                    <div className="code-editor-toolbar">
                      <span className="code-editor-lang">{detectMonacoLang(q.languages)}</span>
                      <div className="code-editor-actions">
                        {q.starterCode && (
                          <button
                            type="button"
                            className="code-editor-reset"
                            onClick={() => handleResetStarterCode(q)}
                          >
                            Resetează scheletul
                          </button>
                        )}
                        <span className="code-editor-hint">Completează funcția</span>
                      </div>
                    </div>
                    <Editor
                      height="360px"
                      language={detectMonacoLang(q.languages)}
                      theme="vs-dark"
                      value={textAnswers[q.interviewQuestionId] || ""}
                      onChange={(val) => handleTextAnswer(q.interviewQuestionId, val ?? "")}
                      options={{
                        fontSize: 14,
                        minimap: { enabled: false },
                        scrollBeyondLastLine: false,
                        wordWrap: "on",
                        tabSize: 2,
                        automaticLayout: true,
                        padding: { top: 12, bottom: 12 },
                        lineNumbersMinChars: 3,
                        renderLineHighlight: "all",
                        smoothScrolling: true,
                      }}
                    />
                  </div>

                  <RunPanel
                    testCases={testCases[q.interviewQuestionId] ?? []}
                    runResult={runResults[q.interviewQuestionId] ?? null}
                    running={runningFor === q.interviewQuestionId}
                    onRun={() => handleRunCode(q)}
                  />
                </>
              )}

              {q.answerType === "MCQ" && (
                <div className="mcq-options">
                  {q.options.map((option) => {
                    const selected = selectedOptions[q.interviewQuestionId] === option.id;
                    return (
                      <label
                        key={option.id}
                        className={`mcq-option ${selected ? "mcq-option-selected" : ""}`}
                      >
                        <input
                          type="radio"
                          name={`q-${q.interviewQuestionId}`}
                          checked={selected}
                          onChange={() => handleOptionSelect(q.interviewQuestionId, option.id)}
                        />
                        <span>{option.text}</span>
                      </label>
                    );
                  })}
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="interview-actions">
          <button className="btn btn-outline" onClick={() => navigate("/")}>Abandonează</button>
          <button className="btn btn-primary" onClick={handleFinish} disabled={finishing}>
            Finalizează interviul →
          </button>
        </div>
      </div>

      {finishing && (
        <div className="interview-finish-overlay">
          <div className="interview-finish-modal">
            <div className="interview-finish-spinner" />
            <p className="interview-finish-title">Se generează feedback-ul...</p>
            <p className="interview-finish-sub">Acest proces poate dura câteva secunde. Te rugăm să aștepți.</p>
          </div>
        </div>
      )}
    </PageLayout>
  );
}
