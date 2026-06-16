import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/api";
import { getMyProfile, updateMyProfile } from "../api/profile";
import PageLayout from "../components/PageLayout";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, ReferenceLine,
} from "recharts";
import "./ProfilePage.css";

type FilterItem = { id: number; name: string };
type HistoryItem = {
  interviewId: number;
  status: string;
  score: number | null;
  createdAt: string;
  level: string;
  position: string;
};

export default function ProfilePage() {
  const navigate = useNavigate();

  // ── Profil ──
  const [email, setEmail]         = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName]   = useState("");
  const [levels, setLevels]       = useState<FilterItem[]>([]);
  const [positions, setPositions] = useState<FilterItem[]>([]);
  const [levelId, setLevelId]     = useState<number | "">("");
  const [positionId, setPositionId] = useState<number | "">("");
  const [loading, setLoading]     = useState(true);
  const [saving, setSaving]       = useState(false);
  const [profileError, setProfileError]   = useState("");
  const [profileSuccess, setProfileSuccess] = useState("");

  // ── Parolă ──
  const [currentPwd, setCurrentPwd]   = useState("");
  const [newPwd, setNewPwd]           = useState("");
  const [confirmPwd, setConfirmPwd]   = useState("");
  const [pwdSaving, setPwdSaving]     = useState(false);
  const [pwdError, setPwdError]       = useState("");
  const [pwdSuccess, setPwdSuccess]   = useState("");

  // ── Istoric / Progres ──
  const [history, setHistory] = useState<HistoryItem[]>([]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const [filtersRes, profile, histRes] = await Promise.all([
          api.get<{ levels: FilterItem[]; positions: FilterItem[] }>("/api/interview/filters"),
          getMyProfile(),
          api.get<HistoryItem[]>("/api/interview/history"),
        ]);
        if (cancelled) return;
        setLevels(filtersRes.data.levels);
        setPositions(filtersRes.data.positions);
        setEmail(profile.email);
        setFirstName(profile.firstName ?? "");
        setLastName(profile.lastName ?? "");
        setLevelId(profile.levelId ?? "");
        setPositionId(profile.positionId ?? "");
        setHistory(histRes.data);
      } catch (err: any) {
        if (!cancelled) setProfileError(err?.response?.data?.message || "Nu am putut încărca profilul.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  // Progres chart — interviuri finalizate cu scor, ordonate cronologic
  const chartData = history
    .filter(i => i.status === "COMPLETED" && i.score !== null)
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    .map((i, idx) => ({
      label: `#${idx + 1}`,
      score: i.score!,
      date: new Date(i.createdAt).toLocaleDateString("ro-RO", { day: "2-digit", month: "short" }),
    }));

  const avgScore = chartData.length > 0
    ? Math.round(chartData.reduce((s, d) => s + d.score, 0) / chartData.length)
    : null;

  async function onSaveProfile() {
    setProfileError("");
    setProfileSuccess("");
    if (!firstName.trim() || !lastName.trim() || !levelId || !positionId) {
      setProfileError("Completează toate câmpurile.");
      return;
    }
    setSaving(true);
    try {
      const updated = await updateMyProfile({
        firstName: firstName.trim(),
        lastName:  lastName.trim(),
        levelId:   Number(levelId),
        positionId: Number(positionId),
      });
      setFirstName(updated.firstName ?? "");
      setLastName(updated.lastName ?? "");
      setLevelId(updated.levelId ?? "");
      setPositionId(updated.positionId ?? "");
      const raw = localStorage.getItem("user");
      if (raw) {
        const u = JSON.parse(raw);
        localStorage.setItem("user", JSON.stringify({ ...u, firstName: updated.firstName, lastName: updated.lastName }));
        window.dispatchEvent(new Event("user-updated"));
      }
      setProfileSuccess("Profil salvat cu succes!");
      setTimeout(() => setProfileSuccess(""), 2000);
    } catch (err: any) {
      setProfileError(err?.response?.data?.message || "Eroare la salvare.");
    } finally {
      setSaving(false);
    }
  }

  async function onChangePassword() {
    setPwdError("");
    setPwdSuccess("");
    if (!currentPwd || !newPwd || !confirmPwd) {
      setPwdError("Completează toate câmpurile.");
      return;
    }
    if (newPwd !== confirmPwd) {
      setPwdError("Parolele nu coincid.");
      return;
    }
    if (newPwd.length < 6) {
      setPwdError("Parola nouă trebuie să aibă minim 6 caractere.");
      return;
    }
    setPwdSaving(true);
    try {
      await api.put("/api/profile/password", {
        currentPassword: currentPwd,
        newPassword:     newPwd,
        confirmPassword: confirmPwd,
      });
      setPwdSuccess("Parola a fost schimbată cu succes!");
      setCurrentPwd("");
      setNewPwd("");
      setConfirmPwd("");
      setTimeout(() => setPwdSuccess(""), 3000);
    } catch (err: any) {
      setPwdError(err?.response?.data?.message || "Eroare la schimbarea parolei.");
    } finally {
      setPwdSaving(false);
    }
  }

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (!active || !payload?.length) return null;
    return (
      <div className="prof-tooltip">
        <p className="prof-tooltip-label">{payload[0]?.payload?.date}</p>
        <p style={{ color: "#6d28d9", margin: 0, fontWeight: 700 }}>
          Scor: {payload[0].value}%
        </p>
      </div>
    );
  };

  return (
    <PageLayout>
      <div className="profilePage">
        <div className="profileContainer">
          {/* Header */}
          <div className="profileHeader">
            <div>
              <h1 className="profileTitle">Profilul meu</h1>
              <p className="profileSubtitle">Date personale, parolă și progresul tău în timp.</p>
            </div>
            <button className="btnOutline" onClick={() => navigate("/")}>Înapoi</button>
          </div>

          {loading ? (
            <div className="card cardPad"><p>Se încarcă profilul...</p></div>
          ) : (
            <>
              {/* ── Rândul 1: Date personale (unified) ── */}
              <div className="card cardPad profile-main-card">
                <h3 className="sectionTitle">Date personale</h3>
                <p className="sectionSubtitle">Poți modifica numele, prenumele și profilul profesional oricând.</p>

                <div className="profileFormGrid">
                  <div className="field">
                    <label className="label">Nume</label>
                    <input className="input" value={lastName} onChange={e => setLastName(e.target.value)} />
                  </div>
                  <div className="field">
                    <label className="label">Prenume</label>
                    <input className="input" value={firstName} onChange={e => setFirstName(e.target.value)} />
                  </div>
                </div>

                <div className="field">
                  <label className="label">Email</label>
                  <input className="input" value={email} disabled />
                </div>

                <div className="profileFormGrid">
                  <div className="field">
                    <label className="label">Nivel</label>
                    <select className="input" value={levelId} onChange={e => setLevelId(Number(e.target.value))}>
                      <option value="">Selectează nivelul</option>
                      {levels.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
                    </select>
                  </div>
                  <div className="field">
                    <label className="label">Poziție ocupată</label>
                    <select className="input" value={positionId} onChange={e => setPositionId(Number(e.target.value))}>
                      <option value="">Selectează poziția</option>
                      {positions.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select>
                  </div>
                </div>

                {profileError   && <div className="errorBox">{profileError}</div>}
                {profileSuccess && <div className="successBox">{profileSuccess}</div>}
                <div className="rowActions">
                  <button className="btnPrimary" onClick={onSaveProfile} disabled={saving}>
                    {saving ? "Se salvează..." : "Salvează modificările"}
                  </button>
                </div>
              </div>

              {/* ── Rândul 2: Schimbare parolă ── */}
              <div className="card cardPad">
                <h3 className="sectionTitle">Schimbă parola</h3>
                <p className="sectionSubtitle">Folosește o parolă puternică de minim 6 caractere.</p>
                <div className="pwdGrid">
                  <div className="field">
                    <label className="label">Parola curentă</label>
                    <input
                      className="input"
                      type="password"
                      placeholder="••••••"
                      value={currentPwd}
                      onChange={e => setCurrentPwd(e.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label className="label">Parola nouă</label>
                    <input
                      className="input"
                      type="password"
                      placeholder="Min. 6 caractere"
                      value={newPwd}
                      onChange={e => setNewPwd(e.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label className="label">Confirmă parola nouă</label>
                    <input
                      className="input"
                      type="password"
                      placeholder="Repetă parola"
                      value={confirmPwd}
                      onChange={e => setConfirmPwd(e.target.value)}
                    />
                  </div>
                </div>
                {pwdError   && <div className="errorBox"  style={{ marginTop: 12 }}>{pwdError}</div>}
                {pwdSuccess && <div className="successBox" style={{ marginTop: 12 }}>{pwdSuccess}</div>}
                <div className="rowActions" style={{ marginTop: 16 }}>
                  <button className="btnPrimary" onClick={onChangePassword} disabled={pwdSaving}>
                    {pwdSaving ? "Se schimbă..." : "Schimbă parola"}
                  </button>
                </div>
              </div>

              {/* ── Rândul 3: Grafic progres ── */}
              <div className="card cardPad">
                <div className="prof-chart-header">
                  <div>
                    <h3 className="sectionTitle">Evoluția scorurilor</h3>
                    <p className="sectionSubtitle">Scorurile obținute la fiecare interviu finalizat.</p>
                  </div>
                  {avgScore !== null && (
                    <div className="prof-avg-badge">
                      <span className="prof-avg-label">Scor mediu</span>
                      <span className={`prof-avg-value ${avgScore >= 70 ? "prof-avg-good" : avgScore >= 40 ? "prof-avg-mid" : "prof-avg-low"}`}>
                        {avgScore}%
                      </span>
                    </div>
                  )}
                </div>

                {chartData.length === 0 ? (
                  <div className="prof-chart-empty">
                    <p>Nu ai interviuri finalizate cu scor încă.</p>
                    <button className="btnPrimary" onClick={() => navigate("/interview/setup")}>
                      Începe un interviu
                    </button>
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <LineChart data={chartData} margin={{ top: 12, right: 24, left: 0, bottom: 8 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(109,40,217,0.08)" />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 12, fill: "var(--clr-text-muted)" }}
                      />
                      <YAxis
                        domain={[0, 100]}
                        tick={{ fontSize: 12, fill: "var(--clr-text-muted)" }}
                        unit="%"
                      />
                      <Tooltip content={<CustomTooltip />} />
                      {avgScore !== null && (
                        <ReferenceLine
                          y={avgScore}
                          stroke="rgba(109,40,217,0.35)"
                          strokeDasharray="6 3"
                          label={{ value: `Medie: ${avgScore}%`, position: "insideTopRight", fontSize: 11, fill: "var(--clr-primary)" }}
                        />
                      )}
                      <Line
                        type="monotone"
                        dataKey="score"
                        name="Scor"
                        stroke="var(--clr-primary)"
                        strokeWidth={2.5}
                        dot={{ r: 5, fill: "var(--clr-primary)", strokeWidth: 2, stroke: "white" }}
                        activeDot={{ r: 7 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </PageLayout>
  );
}
