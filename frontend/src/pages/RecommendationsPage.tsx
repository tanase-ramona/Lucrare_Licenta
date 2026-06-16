import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/api";
import PageLayout from "../components/PageLayout";
// @ts-ignore
import "./RecommendationsPage.css";

// ─────────────────────────────────────────────────────────────
// Baza de resurse statice
// ─────────────────────────────────────────────────────────────
type Resource = {
  title: string;
  url: string;
  description: string;
  category: string;
  tags: string[]; // cuvinte-cheie pentru matching cu temele AI
};

const RESOURCES: Resource[] = [
  // ── Java ────────────────────────────────────────────────────
  {
    title: "Java — Ghid oficial Oracle",
    url: "https://docs.oracle.com/en/java/",
    description: "Documentația oficială Java — API, tutoriale, specificații.",
    category: "Java",
    tags: ["java", "java basics", "java fundamentals", "java core", "java sintaxă"],
  },
  {
    title: "Design Patterns — Refactoring Guru",
    url: "https://refactoring.guru/design-patterns",
    description: "Ghid vizual complet pentru toate patternurile de design cu exemple reale.",
    category: "Design Patterns",
    tags: ["design pattern", "patterns", "singleton", "factory", "observer", "strategy", "decorator pattern", "patternuri"],
  },
  {
    title: "Java Collections Framework — GeeksForGeeks",
    url: "https://www.geeksforgeeks.org/collections-in-java-2/",
    description: "ArrayList, LinkedList, HashMap, TreeMap, Set — explicații și exemple.",
    category: "Java",
    tags: ["collections", "collection", "arraylist", "hashmap", "hashset", "treemap", "linkedlist", "structuri de date java"],
  },
  {
    title: "Java Streams API — Baeldung",
    url: "https://www.baeldung.com/java-8-streams",
    description: "Ghid practic pentru Stream API, filter, map, reduce, collect.",
    category: "Java",
    tags: ["stream", "streams", "stream api", "lambda", "functional", "map reduce", "java 8"],
  },
  {
    title: "Java Multithreading & Concurrency — Jenkov",
    url: "https://jenkov.com/tutorials/java-concurrency/index.html",
    description: "Tutorial complet: Thread, Executor, synchronized, volatile, locks.",
    category: "Java",
    tags: ["multithreading", "concurrency", "thread", "synchronized", "locks", "executor", "parallel", "concurrent", "fire de execuție"],
  },
  {
    title: "Spring Boot — Documentație oficială",
    url: "https://docs.spring.io/spring-boot/docs/current/reference/html/",
    description: "Documentația oficială Spring Boot — configurare, auto-configuration, starters.",
    category: "Spring",
    tags: ["spring", "spring boot", "spring framework", "dependency injection", "ioc", "bean", "spring mvc"],
  },
  {
    title: "Spring Security — Baeldung",
    url: "https://www.baeldung.com/security-spring",
    description: "Autentificare, autorizare, JWT, OAuth2 cu Spring Security.",
    category: "Spring",
    tags: ["spring security", "security", "jwt", "authentication", "authorization", "oauth", "securitate"],
  },
  {
    title: "JPA & Hibernate — Baeldung",
    url: "https://www.baeldung.com/hibernate-5-spring",
    description: "ORM, entități, relații, JPQL, lazy loading, tranzacții.",
    category: "Java",
    tags: ["jpa", "hibernate", "orm", "entity", "repository", "jpql", "lazy loading", "transaction", "baza de date jpa"],
  },
  {
    title: "Java Exception Handling — Oracle Tutorial",
    url: "https://docs.oracle.com/javase/tutorial/essential/exceptions/",
    description: "Excepții checked/unchecked, try-catch-finally, custom exceptions.",
    category: "Java",
    tags: ["exception", "excepție", "try catch", "error handling", "checked unchecked", "exceptions"],
  },
  {
    title: "SOLID Principles — DigitalOcean",
    url: "https://www.digitalocean.com/community/conceptual-articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design",
    description: "Cele 5 principii SOLID explicate cu exemple Java.",
    category: "OOP",
    tags: ["solid", "single responsibility", "open closed", "liskov", "interface segregation", "dependency inversion", "principii"],
  },
  {
    title: "OOP — Programare Orientată pe Obiecte",
    url: "https://www.geeksforgeeks.org/object-oriented-programming-oops-concept-in-java/",
    description: "Encapsulare, moștenire, polimorfism, abstractizare în Java.",
    category: "OOP",
    tags: ["oop", "object oriented", "orientat pe obiecte", "moștenire", "polimorfism", "encapsulare", "abstractizare", "inheritance"],
  },

  // ── Algoritmi & Structuri de date ───────────────────────────
  {
    title: "Structuri de date — GeeksForGeeks",
    url: "https://www.geeksforgeeks.org/data-structures/",
    description: "Array, LinkedList, Stack, Queue, Tree, Graph, Heap — tutorial complet.",
    category: "Algoritmi",
    tags: ["data structures", "structuri de date", "array", "linked list", "stack", "queue", "tree", "graph", "heap", "trie"],
  },
  {
    title: "Algoritmi de sortare — VisuAlgo",
    url: "https://visualgo.net/en/sorting",
    description: "Vizualizare interactivă a algoritmilor de sortare (Bubble, Quick, Merge, Heap).",
    category: "Algoritmi",
    tags: ["sorting", "sortare", "quicksort", "mergesort", "bubblesort", "heapsort", "algoritmi de sortare"],
  },
  {
    title: "Big O Notation — InterviewCake",
    url: "https://www.interviewcake.com/article/python/big-o-notation-time-and-space-complexity",
    description: "Complexitate timp și spațiu — explicație vizuală clară.",
    category: "Algoritmi",
    tags: ["big o", "complexity", "complexitate", "time complexity", "space complexity", "o(n)", "o(log n)", "o(1)"],
  },
  {
    title: "LeetCode — Probleme de programare",
    url: "https://leetcode.com/problemset/",
    description: "Sute de probleme de coding cu soluții și discuții.",
    category: "Algoritmi",
    tags: ["leetcode", "coding problems", "algorithmic problems", "competitive programming", "problem solving", "rezolvare probleme"],
  },
  {
    title: "Dynamic Programming — Neetcode",
    url: "https://neetcode.io/practice",
    description: "Probleme de dynamic programming organizate pe tipare, cu video-soluții.",
    category: "Algoritmi",
    tags: ["dynamic programming", "programare dinamica", "dp", "memoization", "tabulation", "subprobleme"],
  },
  {
    title: "Binary Search — Patterns",
    url: "https://www.geeksforgeeks.org/binary-search/",
    description: "Binary search și variantele sale — upper bound, lower bound.",
    category: "Algoritmi",
    tags: ["binary search", "căutare binară", "search algorithm", "sorted array"],
  },
  {
    title: "Graph Algorithms — CP-Algorithms",
    url: "https://cp-algorithms.com/graph/",
    description: "BFS, DFS, Dijkstra, Bellman-Ford, Floyd-Warshall, MST.",
    category: "Algoritmi",
    tags: ["graph", "graf", "bfs", "dfs", "dijkstra", "shortest path", "minimum spanning tree", "algorimi grafuri"],
  },
  {
    title: "Recursion & Backtracking — GeeksForGeeks",
    url: "https://www.geeksforgeeks.org/recursion/",
    description: "Recursivitate, backtracking, memoizare.",
    category: "Algoritmi",
    tags: ["recursion", "recursivitate", "backtracking", "recursive", "recurent"],
  },

  // ── Baze de date & SQL ───────────────────────────────────────
  {
    title: "SQL — W3Schools Tutorial",
    url: "https://www.w3schools.com/sql/",
    description: "SELECT, JOIN, GROUP BY, subquery — tutorial interactiv complet.",
    category: "Baze de date",
    tags: ["sql", "database", "baza de date", "query", "select", "join", "group by", "subquery", "relational database"],
  },
  {
    title: "PostgreSQL — Documentație oficială",
    url: "https://www.postgresql.org/docs/current/",
    description: "Documentația completă PostgreSQL — tipuri, funcții, indexuri, performanță.",
    category: "Baze de date",
    tags: ["postgresql", "postgres", "pg", "sql avansate", "indexuri", "transactions"],
  },
  {
    title: "Indexuri în baze de date — Use The Index Luke",
    url: "https://use-the-index-luke.com/",
    description: "Ghid practic despre indexuri și optimizarea query-urilor SQL.",
    category: "Baze de date",
    tags: ["index", "indexing", "query optimization", "performanță sql", "sql performance", "slow query"],
  },
  {
    title: "Transactions & ACID — IBM",
    url: "https://www.ibm.com/docs/en/cics-ts/5.4?topic=processing-acid-properties-transactions",
    description: "Proprietățile ACID, izolare tranzacții, deadlock-uri.",
    category: "Baze de date",
    tags: ["transaction", "tranzacție", "acid", "isolation", "atomicity", "consistency", "durability", "deadlock"],
  },

  // ── REST & API ───────────────────────────────────────────────
  {
    title: "REST API Best Practices — Restful API",
    url: "https://restfulapi.net/",
    description: "Principii REST, HTTP methods, status codes, versioning.",
    category: "API",
    tags: ["rest", "rest api", "api", "http", "http methods", "status codes", "versioning", "restful", "endpoint"],
  },
  {
    title: "HTTP Status Codes — MDN",
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status",
    description: "Toate codurile HTTP explicate — 2xx, 3xx, 4xx, 5xx.",
    category: "API",
    tags: ["http status", "status code", "404", "500", "200", "401", "403"],
  },
  {
    title: "OpenAPI & Swagger — Documentație",
    url: "https://swagger.io/docs/",
    description: "Documentarea API-urilor cu OpenAPI/Swagger.",
    category: "API",
    tags: ["swagger", "openapi", "api documentation", "documentare api"],
  },

  // ── Python ──────────────────────────────────────────────────
  {
    title: "Python — Documentație oficială",
    url: "https://docs.python.org/3/",
    description: "Documentația oficială Python 3 — sintaxă, bibliotecă standard, tutoriale.",
    category: "Python",
    tags: ["python", "python basics", "python fundamentals", "python 3", "python sintaxă"],
  },
  {
    title: "Real Python — Tutoriale avansate",
    url: "https://realpython.com/",
    description: "Tutoriale Python practice: decorators, generators, asyncio, testing.",
    category: "Python",
    tags: ["python decorator", "generator", "asyncio", "python async", "python avansate", "python tips"],
  },
  {
    title: "Python Data Structures — GeeksForGeeks",
    url: "https://www.geeksforgeeks.org/python-data-structures/",
    description: "List, dict, tuple, set, deque în Python.",
    category: "Python",
    tags: ["python list", "python dict", "python tuple", "python set", "python collections", "structuri python"],
  },

  // ── JavaScript & Frontend ────────────────────────────────────
  {
    title: "JavaScript — MDN Web Docs",
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide",
    description: "Ghidul complet JavaScript de la MDN — de la bază la avansat.",
    category: "JavaScript",
    tags: ["javascript", "js", "javascript basics", "javascript fundamentals", "es6", "ecmascript"],
  },
  {
    title: "JavaScript — The Modern Tutorial",
    url: "https://javascript.info/",
    description: "Tutorial modern și complet JavaScript: closures, promises, async/await, prototype.",
    category: "JavaScript",
    tags: ["closure", "promises", "async await", "prototype", "event loop", "javascript avansat", "hoisting"],
  },
  {
    title: "TypeScript — Documentație oficială",
    url: "https://www.typescriptlang.org/docs/",
    description: "Tipuri, interfaces, generics, decorators în TypeScript.",
    category: "JavaScript",
    tags: ["typescript", "ts", "typescript types", "generics typescript", "interface typescript"],
  },
  {
    title: "React — Documentație oficială",
    url: "https://react.dev/",
    description: "Documentația oficială React — hooks, state, context, performance.",
    category: "Frontend",
    tags: ["react", "reactjs", "hooks", "usestate", "useeffect", "context", "jsx", "component"],
  },
  {
    title: "CSS — MDN Web Docs",
    url: "https://developer.mozilla.org/en-US/docs/Web/CSS",
    description: "Flexbox, Grid, animații, responsive design.",
    category: "Frontend",
    tags: ["css", "flexbox", "grid", "responsive", "html", "layout", "web design", "stilizare"],
  },

  // ── C / C++ ──────────────────────────────────────────────────
  {
    title: "C++ — cppreference.com",
    url: "https://en.cppreference.com/w/",
    description: "Referință completă C++: STL, smart pointers, templates, move semantics.",
    category: "C++",
    tags: ["c++", "cpp", "stl", "smart pointer", "unique_ptr", "vector", "template", "move semantics", "raii"],
  },
  {
    title: "C — Learn-C.org",
    url: "https://www.learn-c.org/",
    description: "Tutorial interactiv C — pointeri, structuri, alocare dinamică.",
    category: "C",
    tags: ["c language", "c programming", "pointer", "malloc", "struct", "alocare dinamica", "limbaj c"],
  },
  {
    title: "Pointer Basics în C — GeeksForGeeks",
    url: "https://www.geeksforgeeks.org/pointers-in-c-and-c-set-1-introduction-arithmetic-and-array/",
    description: "Pointeri, aritmetică pointeri, pointeri la funcții.",
    category: "C",
    tags: ["pointeri", "pointers", "memory", "memorie", "dereferencing", "pointer arithmetic"],
  },

  // ── Testing ──────────────────────────────────────────────────
  {
    title: "JUnit 5 — Documentație oficială",
    url: "https://junit.org/junit5/docs/current/user-guide/",
    description: "Unit testing în Java cu JUnit 5 — adnotări, assertions, mocking.",
    category: "Testing",
    tags: ["junit", "unit test", "testing", "tdd", "test driven", "testare", "mock", "mockito"],
  },
  {
    title: "Testing în Python — pytest docs",
    url: "https://docs.pytest.org/en/stable/",
    description: "Framework pytest pentru testare Python — fixtures, parametrize.",
    category: "Testing",
    tags: ["pytest", "python testing", "unit test python", "testare python"],
  },

  // ── Git & DevOps ─────────────────────────────────────────────
  {
    title: "Git — Pro Git Book (gratuit)",
    url: "https://git-scm.com/book/en/v2",
    description: "Cartea oficială Git — branching, merging, rebase, workflows.",
    category: "DevOps",
    tags: ["git", "version control", "branch", "merge", "rebase", "commit", "git flow"],
  },
  {
    title: "Docker — Documentație oficială",
    url: "https://docs.docker.com/get-started/",
    description: "Containere Docker — Dockerfile, docker-compose, volumes.",
    category: "DevOps",
    tags: ["docker", "container", "dockerfile", "docker-compose", "containerizare"],
  },

  // ── System Design ────────────────────────────────────────────
  {
    title: "System Design Primer — GitHub",
    url: "https://github.com/donnemartin/system-design-primer",
    description: "Ghid complet de system design — scalability, caching, load balancing, databases.",
    category: "Arhitectură",
    tags: ["system design", "scalability", "microservices", "load balancing", "caching", "arquitectura", "arhitectura"],
  },
  {
    title: "Microservices — Martin Fowler",
    url: "https://martinfowler.com/articles/microservices.html",
    description: "Articolul original despre microservicii de Martin Fowler.",
    category: "Arhitectură",
    tags: ["microservices", "microservicii", "distributed systems", "sisteme distribuite", "soa", "service"],
  },
  {
    title: "Clean Code — rezumat principii",
    url: "https://www.freecodecamp.org/news/clean-coding-for-beginners/",
    description: "Principii clean code: naming, functions, comments, formatting.",
    category: "Bune practici",
    tags: ["clean code", "cod curat", "refactoring", "code quality", "calitatea codului", "naming", "readability"],
  },
];

// ─────────────────────────────────────────────────────────────
// Extrage temele principale ale unui interviu specific
// ─────────────────────────────────────────────────────────────
function extractTopics(item: RecommendationItem): string[] {
  const combined = [item.recommendedTopics, item.recommendedProblemCategories]
    .filter(Boolean).join(", ");
  return combined
    .split(/[,;]/)
    .map((t) => t.trim())
    .filter((t) => t && t.toLowerCase() !== "n/a" && t.length > 2)
    .slice(0, 5);
}

// ─────────────────────────────────────────────────────────────
// Matching: extrage resurse relevante din temele AI
// ─────────────────────────────────────────────────────────────
function findResources(items: RecommendationItem[]): Resource[] {
  const topics: string[] = [];
  for (const item of items) {
    const combined = [item.recommendedTopics, item.recommendedProblemCategories, item.nextSteps]
      .filter(Boolean).join(", ");
    for (const raw of combined.split(/[,;]/)) {
      const t = raw.trim().toLowerCase();
      if (t && t !== "n/a" && t.length > 2) topics.push(t);
    }
  }

  const matched = new Set<string>();
  const result: Resource[] = [];

  for (const resource of RESOURCES) {
    if (matched.has(resource.url)) continue;
    const matches = resource.tags.some(tag =>
      topics.some(topic => topic.includes(tag) || tag.includes(topic))
    );
    if (matches) {
      result.push(resource);
      matched.add(resource.url);
    }
  }

  // Dacă nu s-a potrivit nimic, arată resurse generale
  if (result.length === 0) {
    return RESOURCES.filter(r =>
      ["Algoritmi", "Bune practici", "API"].includes(r.category)
    ).slice(0, 6);
  }

  return result;
}

// ─────────────────────────────────────────────────────────────
// Tipuri
// ─────────────────────────────────────────────────────────────
type RecommendationItem = {
  interviewId: number;
  position: string;
  level: string;
  createdAt: string;
  score: number | null;
  readyLevel: string | null;
  recommendedTopics: string | null;
  recommendedProblemCategories: string | null;
  nextSteps: string | null;
};

const READY_LEVEL_LABELS: Record<string, string> = {
  READY: "Pregătit",
  PARTIALLY_READY: "Parțial pregătit",
  NOT_READY: "Nepregătit",
};

const READY_LEVEL_COLORS: Record<string, string> = {
  READY: "#16a34a",
  PARTIALLY_READY: "#d97706",
  NOT_READY: "#dc2626",
};

const CATEGORY_COLORS: Record<string, string> = {
  "Java": "#4f46e5",
  "Spring": "#059669",
  "Design Patterns": "#7c3aed",
  "OOP": "#0891b2",
  "Algoritmi": "#d97706",
  "Baze de date": "#dc2626",
  "API": "#0ea5e9",
  "Python": "#2563eb",
  "JavaScript": "#ca8a04",
  "Frontend": "#06b6d4",
  "C++": "#7c3aed",
  "C": "#6b7280",
  "Testing": "#16a34a",
  "DevOps": "#9333ea",
  "Arhitectură": "#b45309",
  "Bune practici": "#0f766e",
};

// ─────────────────────────────────────────────────────────────
// Componentă principală
// ─────────────────────────────────────────────────────────────
export default function RecommendationsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<RecommendationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState<"materiale" | "interviuri">("materiale");

  useEffect(() => {
    api.get<RecommendationItem[]>("/api/interview/recommendations")
      .then((res) => setItems(res.data))
      .catch(() => setError("Nu am putut încărca recomandările."))
      .finally(() => setLoading(false));
  }, []);

  const resources = useMemo(() => findResources(items), [items]);

  if (loading) return <PageLayout><div className="rec-loading">Se încarcă recomandările...</div></PageLayout>;
  if (error)   return (
    <PageLayout>
      <div className="rec-error">
        <div className="error-box">{error}</div>
        <button className="btn btn-outline" onClick={() => navigate("/")}>Înapoi acasă</button>
      </div>
    </PageLayout>
  );

  return (
    <PageLayout>
      <div className="rec-container">
        {/* Hero */}
        <div className="rec-hero">
          <div>
            <h1>Recomandări personalizate</h1>
            <p>Bazate pe toate interviurile tale finalizate</p>
          </div>
          <button className="btn btn-outline rec-back-btn" onClick={() => navigate("/")}>
            Înapoi acasă
          </button>
        </div>

        {items.length === 0 ? (
          <div className="card rec-empty">
            <p>Nu ai încă niciun interviu finalizat cu feedback AI.</p>
            <button className="btn btn-primary" onClick={() => navigate("/interview/setup")}>
              Începe primul interviu
            </button>
          </div>
        ) : (
          <>
            {/* Tab bar */}
            <div className="rec-tabs">
              <button
                className={`rec-tab ${activeTab === "materiale" ? "rec-tab-active" : ""}`}
                onClick={() => setActiveTab("materiale")}
              >
                Materiale
                {resources.length > 0 && (
                  <span className="rec-tab-count">{resources.length}</span>
                )}
              </button>
              <button
                className={`rec-tab ${activeTab === "interviuri" ? "rec-tab-active" : ""}`}
                onClick={() => setActiveTab("interviuri")}
              >
                Interviuri de refăcut
                <span className="rec-tab-count">{items.length}</span>
              </button>
            </div>

            {/* Tab: Materiale */}
            {activeTab === "materiale" && (
              resources.length > 0 ? (
                <div className="card rec-resources-card">
                  <div className="rec-resources-header">
                    <div>
                      <h2>Resurse de studiu recomandate</h2>
                      <p className="rec-subtitle">
                        Materiale selectate pe baza temelor identificate în feedback-ul tău.
                      </p>
                    </div>
                    <span className="rec-resources-count">{resources.length} resurse</span>
                  </div>

                  <div className="rec-resources-table-wrap">
                    <table className="rec-resources-table">
                      <thead>
                        <tr>
                          <th>Categorie</th>
                          <th>Resursă</th>
                          <th>Descriere</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {resources.map((r) => (
                          <tr key={r.url}>
                            <td>
                              <span
                                className="rec-cat-badge"
                                style={{ background: `${CATEGORY_COLORS[r.category] ?? "#6b7280"}18`, color: CATEGORY_COLORS[r.category] ?? "#6b7280", borderColor: `${CATEGORY_COLORS[r.category] ?? "#6b7280"}35` }}
                              >
                                {r.category}
                              </span>
                            </td>
                            <td className="rec-res-title">{r.title}</td>
                            <td className="rec-res-desc">{r.description}</td>
                            <td>
                              <a
                                href={r.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="rec-res-link"
                              >
                                Deschide -&gt;
                              </a>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : (
                <div className="card rec-empty">
                  <p>Nu s-au identificat materiale relevante din feedback-ul tau.</p>
                </div>
              )
            )}

            {/* Tab: Interviuri de refacut */}
            {activeTab === "interviuri" && (
              <div className="rec-retake-list">
                {items.map((item) => {
                  const topics = extractTopics(item);
                  const dateStr = new Date(item.createdAt).toLocaleDateString("ro-RO", {
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                  });
                  const rl = item.readyLevel ?? "";
                  return (
                    <div key={item.interviewId} className="card rec-retake-card">
                      <div className="rec-retake-header">
                        <div className="rec-retake-meta">
                          <span className="rec-retake-pos">{item.position}</span>
                          <span className="rec-retake-sep">·</span>
                          <span className="rec-retake-level">{item.level}</span>
                          <span className="rec-retake-date">{dateStr}</span>
                        </div>
                        <div className="rec-retake-badges">
                          {item.score != null && (
                            <span className="rec-retake-score">{item.score}%</span>
                          )}
                          {rl && (
                            <span
                              className="rec-retake-rl"
                              style={{
                                background: `${READY_LEVEL_COLORS[rl] ?? "#6b7280"}18`,
                                color: READY_LEVEL_COLORS[rl] ?? "#6b7280",
                                borderColor: `${READY_LEVEL_COLORS[rl] ?? "#6b7280"}40`,
                              }}
                            >
                              {READY_LEVEL_LABELS[rl] ?? rl}
                            </span>
                          )}
                        </div>
                      </div>

                      {topics.length > 0 && (
                        <div className="rec-retake-callout">
                          <span>
                            Dacă ai parcurs materialele despre{" "}
                            <strong>{topics.slice(0, 3).join(", ")}</strong>
                            {topics.length > 3 ? " și altele" : ""},
                            ești pregătit să redai acest interviu!
                          </span>
                        </div>
                      )}

                      {topics.length > 0 && (
                        <div className="rec-topic-chips">
                          {topics.map((t) => (
                            <span key={t} className="rec-topic-chip">{t}</span>
                          ))}
                        </div>
                      )}

                      <div className="rec-retake-actions">
                        <button
                          className="btn btn-primary"
                          onClick={() => navigate("/interview/setup")}
                        >
                          Reia interviul →
                        </button>
                        <button
                          className="btn btn-outline"
                          onClick={() => navigate(`/interview/${item.interviewId}/review`)}
                        >
                          Revezi recenzia
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </>
        )}
      </div>
    </PageLayout>
  );
}
