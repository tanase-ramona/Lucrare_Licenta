import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { api } from "../api/api";
import {
  PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  LineChart, Line,
} from "recharts";
// @ts-ignore
import "./AdminDashboard.css";

type OverviewDto = {
  totalUsers: number;
  totalInterviews: number;
  completedInterviews: number;
  avgScore: number;
  activeUsersInPeriod: number;
  interviewsInPeriod: number;
  completedInPeriod: number;
};
type NameValue = { name: string; value: number };
type DayCount  = { date: string; count: number };
type DayScore  = { date: string; score: number };

type AdminStats = {
  overview: OverviewDto;
  positionStats: NameValue[];
  levelDistribution: NameValue[];
  languageScores: NameValue[];
  categoryPerformance: NameValue[];
  interviewsOverTime: DayCount[];
  avgScoreOverTime: DayScore[];
  successRate: number;
};

type Period = "1m" | "6m" | "1y";
const PERIOD_LABELS: Record<Period, string> = {
  "1m": "Ultima lună",
  "6m": "Ultimele 6 luni",
  "1y": "Ultimul an",
};

const COLORS = ["#4f46e5", "#7c3aed", "#06b6d4", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"];

function StatCard({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="adm-stat-card card">
      <div>
        <p className="adm-stat-label">{label}</p>
        <p className="adm-stat-value">{value}</p>
        {sub && <p className="adm-stat-sub">{sub}</p>}
      </div>
    </div>
  );
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="adm-chart-card card">
      <h3 className="adm-chart-title">{title}</h3>
      {children}
    </div>
  );
}

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="adm-tooltip">
      {label && <p className="adm-tooltip-label">{label}</p>}
      {payload.map((p: any, i: number) => (
        <p key={i} style={{ color: p.color ?? p.fill, margin: 0 }}>
          {p.name}: <strong>{typeof p.value === "number" ? p.value.toFixed(1) : p.value}</strong>
        </p>
      ))}
    </div>
  );
};

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [stats, setStats]     = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState("");
  const [period, setPeriod]   = useState<Period>("1m");

  useEffect(() => {
    setLoading(true);
    setError("");
    api.get<AdminStats>(`/api/admin/stats?period=${period}`)
      .then(r => setStats(r.data))
      .catch(() => setError("Nu s-au putut încărca statisticile."))
      .finally(() => setLoading(false));
  }, [period]);

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="adm-layout">
      {/* ── Sidebar ── */}
      <aside className="adm-sidebar">
        <div className="adm-sidebar-logo">
          <span className="adm-logo-text">Admin Panel</span>
        </div>

        <nav className="adm-nav">
          <button className="adm-nav-item adm-nav-item-active">
            <span>Dashboard</span>
          </button>
          <button className="adm-nav-item" onClick={() => navigate("/admin/users")}>
            <span>Utilizatori</span>
          </button>
          <button className="adm-nav-item" onClick={() => navigate("/admin/questions/manage")}>
            <span>Întrebări</span>
          </button>
          <button className="adm-nav-item" onClick={() => navigate("/admin/questions")}>
            <span>Adaugă întrebare</span>
          </button>
        </nav>

        <div className="adm-sidebar-footer">
          <button className="adm-nav-item" onClick={() => navigate("/")}>
            <span>Înapoi la app</span>
          </button>
          <button className="adm-nav-item adm-logout-btn" onClick={handleLogout}>
            <span>Deconectare</span>
          </button>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className="adm-main">
        <header className="adm-topbar">
          <div>
            <h1 className="adm-topbar-title">Dashboard</h1>
            <p className="adm-topbar-sub">Statistici agregate ale platformei</p>
          </div>
          <div className="adm-topbar-right">
            <div className="adm-period-filter">
              {(["1m", "6m", "1y"] as Period[]).map(p => (
                <button
                  key={p}
                  className={`adm-period-btn ${period === p ? "adm-period-btn-active" : ""}`}
                  onClick={() => setPeriod(p)}
                >
                  {PERIOD_LABELS[p]}
                </button>
              ))}
            </div>
            <span className="badge badge-purple">ADMIN</span>
          </div>
        </header>

        <div className="adm-content">
          {loading && <div className="adm-loading">Se încarcă datele...</div>}
          {error   && <div className="error-box">{error}</div>}

          {stats && (
            <>
              {/* ── Stat cards ── */}
              <div className="adm-stats-grid">
                <StatCard label="Utilizatori totali" value={stats.overview.totalUsers} />
                <StatCard
                  label={`Interviuri — ${PERIOD_LABELS[period].toLowerCase()}`}
                  value={stats.overview.interviewsInPeriod}
                  sub={`Total: ${stats.overview.totalInterviews}`}
                />
                <StatCard
                  label={`Finalizate — ${PERIOD_LABELS[period].toLowerCase()}`}
                  value={stats.overview.completedInPeriod}
                  sub={`Total finalizate: ${stats.overview.completedInterviews}`}
                />
                <StatCard label="Scor mediu global" value={`${stats.overview.avgScore}%`} />
                <StatCard
                  label={`Utilizatori activi — ${PERIOD_LABELS[period].toLowerCase()}`}
                  value={stats.overview.activeUsersInPeriod}
                />
                <StatCard label="Rată de succes" value={`${stats.successRate}%`} sub="scor ≥ 70%" />
              </div>

              {/* ── Row 1: Poziții | Nivele ── */}
              <div className="adm-charts-row">
                <ChartCard title="Interviuri pe poziții">
                  {stats.positionStats.length === 0 ? (
                    <p className="adm-empty">Nu există date.</p>
                  ) : (
                    <ResponsiveContainer width="100%" height={280}>
                      <BarChart data={stats.positionStats} margin={{ top: 8, right: 16, left: 0, bottom: 40 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,100,120,0.15)" />
                        <XAxis dataKey="name" tick={{ fontSize: 12, fill: "var(--clr-text-secondary)" }} angle={-25} textAnchor="end" interval={0} />
                        <YAxis tick={{ fontSize: 12, fill: "var(--clr-text-secondary)" }} allowDecimals={false} />
                        <Tooltip content={<CustomTooltip />} />
                        <Bar dataKey="value" name="Interviuri" radius={[6, 6, 0, 0]}>
                          {stats.positionStats.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  )}
                </ChartCard>

                <ChartCard title="Distribuție nivele candidați">
                  {stats.levelDistribution.length === 0 ? (
                    <p className="adm-empty">Nu există date.</p>
                  ) : (
                    <ResponsiveContainer width="100%" height={280}>
                      <PieChart>
                        <Pie data={stats.levelDistribution} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`}>
                          {stats.levelDistribution.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                        </Pie>
                        <Tooltip content={<CustomTooltip />} />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  )}
                </ChartCard>
              </div>

              {/* ── Row 2: Limbaje | Categorii ── */}
              <div className="adm-charts-row">
                <ChartCard title="Scor mediu pe limbaje (0–10)">
                  {stats.languageScores.length === 0 ? (
                    <p className="adm-empty">Nu există date suficiente.</p>
                  ) : (
                    <ResponsiveContainer width="100%" height={280}>
                      <BarChart data={stats.languageScores} layout="vertical" margin={{ left: 16, right: 24 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,100,120,0.15)" />
                        <XAxis type="number" domain={[0, 10]} tick={{ fontSize: 12, fill: "var(--clr-text-secondary)" }} />
                        <YAxis dataKey="name" type="category" width={80} tick={{ fontSize: 13, fill: "var(--clr-text-secondary)" }} />
                        <Tooltip content={<CustomTooltip />} />
                        <Bar dataKey="value" name="Scor mediu" radius={[0, 6, 6, 0]}>
                          {stats.languageScores.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  )}
                </ChartCard>

                <ChartCard title="Scor mediu pe categorii (0–10)">
                  {stats.categoryPerformance.length === 0 ? (
                    <p className="adm-empty">Nu există date suficiente.</p>
                  ) : (
                    <ResponsiveContainer width="100%" height={280}>
                      <BarChart data={stats.categoryPerformance}>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,100,120,0.15)" />
                        <XAxis dataKey="name" tick={{ fontSize: 13, fill: "var(--clr-text-secondary)" }} />
                        <YAxis domain={[0, 10]} tick={{ fontSize: 12, fill: "var(--clr-text-secondary)" }} />
                        <Tooltip content={<CustomTooltip />} />
                        <Bar dataKey="value" name="Scor mediu" radius={[6, 6, 0, 0]}>
                          {stats.categoryPerformance.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  )}
                </ChartCard>
              </div>

              {/* ── Evoluție scoruri medii ── */}
              <ChartCard title={`Evoluție scoruri medii — ${PERIOD_LABELS[period]}`}>
                {stats.avgScoreOverTime.length === 0 ? (
                  <p className="adm-empty">Nu există date suficiente pentru perioada selectată.</p>
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <LineChart data={stats.avgScoreOverTime} margin={{ top: 8, right: 24, left: 0, bottom: 50 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,100,120,0.15)" />
                      <XAxis dataKey="date" tick={{ fontSize: 11, fill: "var(--clr-text-secondary)" }} angle={-35} textAnchor="end" interval="preserveStartEnd" />
                      <YAxis domain={[0, 100]} tick={{ fontSize: 12, fill: "var(--clr-text-secondary)" }} unit="%" />
                      <Tooltip content={<CustomTooltip />} />
                      <Line type="monotone" dataKey="score" name="Scor mediu (%)" stroke="#10b981" strokeWidth={2.5} dot={{ r: 4, fill: "#10b981" }} activeDot={{ r: 6 }} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </ChartCard>

              {/* ── Interviuri create în timp ── */}
              <ChartCard title={`Interviuri create — ${PERIOD_LABELS[period]}`}>
                {stats.interviewsOverTime.length === 0 ? (
                  <p className="adm-empty">Nu există date suficiente pentru perioada selectată.</p>
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <LineChart data={stats.interviewsOverTime} margin={{ top: 8, right: 24, left: 0, bottom: 50 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,100,120,0.15)" />
                      <XAxis dataKey="date" tick={{ fontSize: 11, fill: "var(--clr-text-secondary)" }} angle={-35} textAnchor="end" interval="preserveStartEnd" />
                      <YAxis tick={{ fontSize: 12, fill: "var(--clr-text-secondary)" }} allowDecimals={false} />
                      <Tooltip content={<CustomTooltip />} />
                      <Line type="monotone" dataKey="count" name="Interviuri" stroke="#4f46e5" strokeWidth={2.5} dot={{ r: 4, fill: "#4f46e5" }} activeDot={{ r: 6 }} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </ChartCard>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
