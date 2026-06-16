import React from "react";
import { Link } from "react-router-dom";
// @ts-ignore
import "./LoginPage.css";

const FEATURES = [
  {
    id: "interviuri",
    title: "Interviuri adaptate nivelului si rolului tau",
    description:
      "Platforma genereaza automat seturi de intrebari personalizate in functie de profilul tau profesional, asigurandu-se ca fiecare sesiune este relevanta si utila.",
    bullets: [
      "Niveluri disponibile: Junior, Mid-level, Senior",
      "Pozitii acoperite: Frontend, Backend, Full Stack, DevOps, Data Science si altele",
      "Fiecare sesiune este unica — intrebarile nu se repeta",
      "Dificultate ajustata in functie de nivelul ales",
    ],
  },
  {
    id: "feedback",
    title: "Feedback AI pentru fiecare raspuns",
    description:
      "Dupa fiecare raspuns, modelul de AI analizeaza in timp real calitatea si completitudinea acestuia, oferind o perspectiva clara asupra punctelor forte si slabe.",
    bullets: [
      "Puncte forte identificate automat in raspunsul tau",
      "Ce informatii lipsesc sau ar imbunatati raspunsul",
      "Sugestii concrete de reformulare sau completare",
      "Scor per intrebare si scor global pe sesiune",
    ],
  },
  {
    id: "limbaje",
    title: "Diversitate de intrebari si limbaje",
    description:
      "Acoperim o gama larga de domenii tehnice, teorice si practice, de la algoritmi clasici pana la tehnologii moderne folosite in industrie.",
    bullets: [
      "Limbaje: Java, Python, JavaScript, TypeScript, C++, SQL si altele",
      "Domenii: algoritmi si structuri de date, baze de date, sisteme de operare",
      "Framework-uri si tehnologii moderne: React, Spring Boot, Node.js",
      "Intrebari teoretice si practice cu exemple de cod real",
    ],
  },
  {
    id: "statistici",
    title: "Statistici si urmarirea progresului",
    description:
      "Monitorizeaza-ti evolutia in timp si identifica rapid punctele slabe, astfel incat sa iti concentrezi eforturile acolo unde conteaza cel mai mult.",
    bullets: [
      "Grafice cu evolutia scorurilor pe sesiuni",
      "Analiza per domeniu: unde excelezi si unde mai ai de lucru",
      "Comparatie intre sesiunile anterioare",
      "Istoric complet al tuturor interviurilor si raspunsurilor",
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
          <h1>Pregatire inteligenta pentru interviuri care conteaza</h1>
          <p>
            Exerseaza cu intrebari reale, primeste feedback instant si
            urmareste-ti progresul pana cand esti cu adevarat pregatit.
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
