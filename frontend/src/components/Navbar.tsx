import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
// @ts-ignore
import "./Navbar.css";

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { state, logout } = useAuth();

  const email = state.user?.email ?? "";
  const fullName = [state.user?.firstName, state.user?.lastName].filter(Boolean).join(" ");
  const initials = fullName
    ? fullName.split(" ").map((p) => p[0]).join("").slice(0, 2).toUpperCase()
    : email.slice(0, 2).toUpperCase();

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  function isActive(path: string) {
    if (path === "/") return location.pathname === "/";
    return location.pathname.startsWith(path);
  }

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        {/* ── Brand — left ── */}
        <span className="navbar-brand" onClick={() => navigate("/")}>
          IntervYou
        </span>

        {/* ── Nav links — centered ── */}
        <div className="navbar-links">
          <button
            className={`nav-link${isActive("/") ? " nav-link-active" : ""}`}
            onClick={() => navigate("/")}
          >
            Home
          </button>

          <button
            className={`nav-link${isActive("/recommendations") ? " nav-link-active" : ""}`}
            onClick={() => navigate("/recommendations")}
          >
            Recomandari
          </button>

          <button
            className={`nav-link nav-link-new${isActive("/interview/setup") ? " nav-link-active" : ""}`}
            onClick={() => navigate("/interview/setup")}
          >
            Interviu Nou
          </button>

          <button
            className={`nav-link${isActive("/profile") ? " nav-link-active" : ""}`}
            onClick={() => navigate("/profile")}
          >
            Profil
          </button>

          {state.user?.roles.includes("ADMIN") && (
            <button
              className={`nav-link nav-link-admin${isActive("/admin") ? " nav-link-active" : ""}`}
              onClick={() => navigate("/admin")}
            >
              Admin
            </button>
          )}
        </div>

        {/* ── Right: logout + avatar ── */}
        <div className="navbar-right">
          <button className="nav-logout" onClick={handleLogout}>
            Deconectare
          </button>
          <div
            className="nav-avatar"
            title={fullName || email}
            onClick={() => navigate("/profile")}
          >
            {initials}
          </div>
        </div>
      </div>
    </nav>
  );
}
