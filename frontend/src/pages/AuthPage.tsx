import React, { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api/api";
import { useAuth } from "../auth/AuthContext";
// @ts-ignore
import "./AuthPage.css";

type FilterItem = { id: number; name: string };

const SPECIAL_CHAR_RE = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/;

export default function AuthPage() {
  const navigate = useNavigate();
  const { login, register } = useAuth();
  const [searchParams] = useSearchParams();

  const [mode, setMode] = useState<"login" | "register">(
    searchParams.get("mode") === "register" ? "register" : "login"
  );
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [levels, setLevels] = useState<FilterItem[]>([]);
  const [positions, setPositions] = useState<FilterItem[]>([]);
  const [levelId, setLevelId] = useState<number | "">("");
  const [positionId, setPositionId] = useState<number | "">("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [showPasswordPopup, setShowPasswordPopup] = useState(false);
  const passwordInfoRef = useRef<HTMLDivElement>(null);

  const passwordCriteria = [
    { label: "Minim 6 caractere", met: password.length >= 6 },
    { label: "Cel puțin o cifră (0–9)", met: /\d/.test(password) },
    { label: "Cel puțin un caracter special (!@#$% etc.)", met: SPECIAL_CHAR_RE.test(password) },
  ];
  const passwordsMatch = password.length > 0 && password === confirmPassword;

  useEffect(() => {
    if (mode !== "register") return;
    if (levels.length > 0) return;
    api
      .get<{ levels: FilterItem[]; positions: FilterItem[] }>("/api/interview/filters")
      .then((res) => {
        setLevels(res.data.levels);
        setPositions(res.data.positions);
      })
      .catch(() => setError("Nu am putut încărca nivelurile și pozițiile."));
  }, [mode]);

  useEffect(() => {
    if (!showPasswordPopup) return;
    function onOutsideClick(e: MouseEvent) {
      if (passwordInfoRef.current && !passwordInfoRef.current.contains(e.target as Node)) {
        setShowPasswordPopup(false);
      }
    }
    document.addEventListener("mousedown", onOutsideClick);
    return () => document.removeEventListener("mousedown", onOutsideClick);
  }, [showPasswordPopup]);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      if (mode === "login") {
        await login(email, password);
      } else {
        if (!passwordCriteria.every((c) => c.met)) {
          setError("Parola nu îndeplinește toate criteriile de securitate.");
          return;
        }
        if (password !== confirmPassword) {
          setError("Parolele nu coincid.");
          return;
        }
        if (!firstName.trim() || !lastName.trim() || !levelId || !positionId) {
          setError("Completează toate câmpurile obligatorii.");
          return;
        }
        await register({
          email,
          password,
          confirmPassword,
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          levelId: Number(levelId),
          positionId: Number(positionId),
        });
      }
      const roles: string[] = JSON.parse(localStorage.getItem("user") || "{}").roles ?? [];
      navigate(roles.includes("ADMIN") ? "/admin" : "/", { replace: true });
    } catch (err: any) {
      setError(err?.response?.data?.message || "A apărut o eroare. Încearcă din nou.");
    } finally {
      setLoading(false);
    }
  }

  function switchMode(m: "login" | "register") {
    setMode(m);
    setError(null);
    setShowPasswordPopup(false);
  }

  return (
    <div className="auth-page">
      {/* Back to landing */}
      <Link to="/login" className="auth-back">
        ← IntervYou
      </Link>

      <div className="auth-card">
        {/* Tabs */}
        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab${mode === "login" ? " auth-tab-active" : ""}`}
            onClick={() => switchMode("login")}
          >
            Autentificare
          </button>
          <button
            type="button"
            className={`auth-tab${mode === "register" ? " auth-tab-active" : ""}`}
            onClick={() => switchMode("register")}
          >
            Înregistrare
          </button>
        </div>

        {/* Form header */}
        <div className="auth-form-header">
          <h2>{mode === "login" ? "Bun venit înapoi" : "Creează un cont"}</h2>
          <p>{mode === "login" ? "Intră în contul tău pentru a continua" : "Completează profilul de bază"}</p>
        </div>

        <form onSubmit={onSubmit} className="auth-form">
          {mode === "register" && (
            <div className="auth-form-row">
              <div className="form-field">
                <label className="form-label">Nume</label>
                <input className="input" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
              </div>
              <div className="form-field">
                <label className="form-label">Prenume</label>
                <input className="input" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
              </div>
            </div>
          )}

          <div className="form-field">
            <label className="form-label">Adresa email</label>
            <input
              className="input"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />
          </div>

          <div className="form-field">
            <div className="form-label-row">
              <label className="form-label">Parola</label>
              <div className="password-info-wrap" ref={passwordInfoRef}>
                <button
                  type="button"
                  className={`password-info-btn${showPasswordPopup ? " password-info-btn-active" : ""}`}
                  onClick={() => setShowPasswordPopup((v) => !v)}
                  title="Cerințe parolă"
                >
                  i
                </button>
                {showPasswordPopup && (
                  <div className="password-criteria-popup">
                    <p className="password-criteria-title">Cerințe parolă</p>
                    {passwordCriteria.map((c, i) => (
                      <div key={i} className={`password-criterion ${c.met ? "criterion-met" : "criterion-unmet"}`}>
                        <span className="criterion-icon">{c.met ? "✓" : "✗"}</span>
                        <span>{c.label}</span>
                      </div>
                    ))}
                    {mode === "register" && (
                      <div className={`password-criterion ${passwordsMatch ? "criterion-met" : "criterion-unmet"}`}>
                        <span className="criterion-icon">{passwordsMatch ? "✓" : "✗"}</span>
                        <span>Parolele coincid</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
            <input
              className="input"
              type="password"
              placeholder="Minim 6 caractere"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete={mode === "login" ? "current-password" : "new-password"}
            />
            {password.length > 0 && (
              <div className="password-criteria-inline">
                {passwordCriteria.map((c, i) => (
                  <div key={i} className={`password-criterion ${c.met ? "criterion-met" : "criterion-unmet"}`}>
                    <span className="criterion-icon">{c.met ? "✓" : "✗"}</span>
                    <span>{c.label}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {mode === "register" && (
            <>
              <div className="form-field">
                <label className="form-label">Confirmare parolă</label>
                <input
                  className="input"
                  type="password"
                  placeholder="Repetă parola"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                />
                {confirmPassword.length > 0 && (
                  <div className={`password-criterion password-match-row ${passwordsMatch ? "criterion-met" : "criterion-unmet"}`}>
                    <span className="criterion-icon">{passwordsMatch ? "✓" : "✗"}</span>
                    <span>Parolele coincid</span>
                  </div>
                )}
              </div>

              <div className="auth-form-row">
                <div className="form-field">
                  <label className="form-label">Nivel</label>
                  <select className="input" value={levelId} onChange={(e) => setLevelId(Number(e.target.value))} required>
                    <option value="">Selectează</option>
                    {levels.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                  </select>
                </div>
                <div className="form-field">
                  <label className="form-label">Poziție ocupată</label>
                  <select className="input" value={positionId} onChange={(e) => setPositionId(Number(e.target.value))} required>
                    <option value="">Selectează</option>
                    {positions.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </select>
                </div>
              </div>
            </>
          )}

          {error && <div className="error-box">{error}</div>}

          <button type="submit" className="btn btn-primary auth-submit-btn" disabled={loading}>
            {loading ? "Se procesează..." : mode === "login" ? "Intră în cont" : "Creează cont"}
          </button>
        </form>
      </div>
    </div>
  );
}
