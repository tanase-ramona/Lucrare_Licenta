import React from "react";
import { Link } from "react-router-dom";
// @ts-ignore
import "./LoginPage.css";

const FEATURES = [
  {
    id: "interviuri",
    title: "Interviuri adaptate nivelului și rolului tău",
    description:
      "Platforma generează automat seturi de întrebări personalizate în funcție de profilul tău profesional, asigurându-se că fiecare sesiune este relevantă și utilă.",
    bullets: [
      "Niveluri disponibile: Junior, Mid-level, Senior",
      "Poziții acoperite: Frontend, Backend, Full Stack, DevOps, Data Science și altele",
      "Fiecare sesiune este unică — întrebările nu se repetă",
      "Dificultate ajustată în funcție de nivelul ales",
    ],
  },
  {
    id: "feedback",
    title: "Feedback AI pentru fiecare răspuns",
    description:
      "După fiecare răspuns, modelul de AI analizează în timp real calitatea și completitudinea acestuia, oferind o perspectivă clară asupra punctelor forte și slabe.",
    bullets: [
      "Puncte forte identificate automat în răspunsul tău",
      "Ce informații lipsesc sau ar îmbunătăți răspunsul",
      "Sugestii concrete de reformulare sau completare",
      "Scor per întrebare și scor global pe sesiune",
    ],
  },
  {
    id: "limbaje",
    title: "Diversitate de întrebări și limbaje",
    description:
      "Acoperim o gamă largă de domenii tehnice, teoretice și practice, de la algoritmi clasici până la tehnologii moderne folosite în industrie.",
    bullets: [
      "Limbaje: Java, Python, JavaScript, TypeScript, C++, SQL și altele",
      "Domenii: algoritmi și structuri de date, baze de date, sisteme de operare",
      "Framework-uri și tehnologii moderne: React, Spring Boot, Node.js",
      "Întrebări teoretice și practice cu exemple de cod real",
    ],
  },
  {
    id: "statistici",
    title: "Statistici și urmărirea progresului",
    description:
      "Monitorizează-ți evoluția în timp și identifică rapid punctele slabe, astfel încât să îți concentrezi eforturile acolo unde contează cel mai mult.",
    bullets: [
      "Grafice cu evoluția scorurilor pe sesiuni",
      "Analiză per domeniu: unde excelezi și unde mai ai de lucru",
      "Comparație între sesiunile anterioare",
      "Istoric complet al tuturor interviurilor și răspunsurilor",
    ],
  },
];

export default function LoginPage() {
  function scrollTo(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
  }

  return (
    <div className="landing-page">
      {/* ── Above the fold ── */}
      <div className="landing-fold">
        <nav className="landing-nav">
          <span className="landing-brand">IntervYou</span>
          <div className="landing-nav-actions">
            <Link to="/auth?mode=login" className="landing-nav-login">
              Log In
            </Link>
            <Link to="/auth?mode=register" className="landing-nav-register btn btn-primary">
              Register
            </Link>
          </div>
        </nav>

        <section className="landing-hero">
          <h1>Pregătire inteligentă pentru interviuri care contează</h1>
          <p>
            Exersează cu întrebări reale, primește feedback instant și
            urmărește-ți progresul până când ești cu adevărat pregătit.
          </p>
        </section>

        <section className="landing-cards">
          {FEATURES.map((feat, idx) => (
            <button
              key={feat.id}
              type="button"
              className="landing-card"
              onClick={() => scrollTo(feat.id)}
            >
              <span className="landing-card-num">0{idx + 1}</span>
              <h3 className="landing-card-title">{feat.title}</h3>
              <span className="landing-card-arrow">↓</span>
            </button>
          ))}
        </section>
      </div>

      {/* ── Detail sections ── */}
      <div className="landing-details">
        {FEATURES.map((feat, idx) => (
          <section key={feat.id} id={feat.id} className="landing-detail-section">
            <div className="landing-detail-inner">
              <span className="landing-detail-num">0{idx + 1}</span>
              <h2 className="landing-detail-title">{feat.title}</h2>
              <p className="landing-detail-desc">{feat.description}</p>
              <ul className="landing-detail-bullets">
                {feat.bullets.map((b, i) => (
                  <li key={i}>{b}</li>
                ))}
              </ul>
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
