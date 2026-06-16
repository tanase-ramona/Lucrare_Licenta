package com.licenta.backend.config;

import com.licenta.backend.interviews.filters.entity.Language;
import com.licenta.backend.interviews.filters.entity.Level;
import com.licenta.backend.interviews.filters.entity.Position;
import com.licenta.backend.interviews.filters.repo.LanguageRepository;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import com.licenta.backend.questions.entity.Question;
import com.licenta.backend.questions.entity.QuestionCategory;
import com.licenta.backend.questions.entity.QuestionOption;
import com.licenta.backend.questions.entity.TestCase;
import com.licenta.backend.questions.repo.QuestionCategoryRepo;
import com.licenta.backend.questions.repo.QuestionRepo;
import com.licenta.backend.questions.repo.TestCaseRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(10)
public class QuestionDataInitializer implements CommandLineRunner {

    private final QuestionCategoryRepo categoryRepo;
    private final QuestionRepo questionRepo;
    private final TestCaseRepo testCaseRepo;
    private final LevelRepository levelRepo;
    private final PositionRepository positionRepo;
    private final LanguageRepository languageRepo;

    // Texte deja existente în DB — populat o singură dată la run()
    private Set<String> existingTexts = new HashSet<>();

    public QuestionDataInitializer(QuestionCategoryRepo categoryRepo,
                                   QuestionRepo questionRepo,
                                   TestCaseRepo testCaseRepo,
                                   LevelRepository levelRepo,
                                   PositionRepository positionRepo,
                                   LanguageRepository languageRepo) {
        this.categoryRepo = categoryRepo;
        this.questionRepo = questionRepo;
        this.testCaseRepo = testCaseRepo;
        this.levelRepo = levelRepo;
        this.positionRepo = positionRepo;
        this.languageRepo = languageRepo;
    }

    @Override
    public void run(String... args) {
        // Categorii
        var hr      = cat("HR");
        var tech    = cat("TECH");
        var problem = cat("PROBLEM");

        // Niveluri
        var intern = level("Intern");
        var junior = level("Junior");
        var mid    = level("Mid");
        var senior = level("Senior");

        // Poziții
        var backend   = pos("Backend");
        var frontend  = pos("Frontend");
        var fullstack = pos("Fullstack");
        var qa        = pos("QA");

        // Limbaje
        var java   = lang("Java");
        var python = lang("Python");
        var js     = lang("JavaScript");
        var cpp    = lang("C++");
        var c      = lang("C");

        if (intern == null || junior == null || mid == null || senior == null) return;
        if (backend == null || frontend == null || fullstack == null || qa == null) return;
        if (java == null || python == null || js == null) return;

        // Index text-uri existente pentru deduplicare
        existingTexts = questionRepo.findAll().stream()
                .map(Question::getText)
                .collect(Collectors.toSet());

        Set<Position> allPos  = Set.of(backend, frontend, fullstack, qa);
        Set<Language> allLang = buildLangSet(java, python, js, cpp, c);
        Set<Language> javaSet = Set.of(java);
        Set<Language> pySet   = Set.of(python);
        Set<Language> jsSet   = Set.of(js);
        Set<Language> cppSet  = Set.of(cpp);
        Set<Language> cSet    = Set.of(c);

        // ─────────────────────────────────────────────────────────────
        // HR — întrebări comportamentale (toate pozițiile, toate limbajele)
        // ─────────────────────────────────────────────────────────────
        seedHR(hr, intern, allPos, allLang);
        seedHR(hr, junior, allPos, allLang);
        seedHR(hr, mid,    allPos, allLang);
        seedHR(hr, senior, allPos, allLang);

        // ─────────────────────────────────────────────────────────────
        // TECH — Java
        // ─────────────────────────────────────────────────────────────
        seedJavaTech(tech, intern, allPos, javaSet);
        seedJavaTech(tech, junior, allPos, javaSet);
        seedJavaTech(tech, mid,    allPos, javaSet);
        seedJavaTech(tech, senior, allPos, javaSet);

        // ─────────────────────────────────────────────────────────────
        // TECH — Python
        // ─────────────────────────────────────────────────────────────
        seedPythonTech(tech, intern, allPos, pySet);
        seedPythonTech(tech, junior, allPos, pySet);
        seedPythonTech(tech, mid,    allPos, pySet);
        seedPythonTech(tech, senior, allPos, pySet);

        // ─────────────────────────────────────────────────────────────
        // TECH — JavaScript
        // ─────────────────────────────────────────────────────────────
        seedJsTech(tech, intern, allPos, jsSet);
        seedJsTech(tech, junior, allPos, jsSet);
        seedJsTech(tech, mid,    allPos, jsSet);
        seedJsTech(tech, senior, allPos, jsSet);

        // ─────────────────────────────────────────────────────────────
        // TECH — C++
        // ─────────────────────────────────────────────────────────────
        seedCppTech(tech, intern, allPos, cppSet);
        seedCppTech(tech, junior, allPos, cppSet);
        seedCppTech(tech, mid,    allPos, cppSet);

        // ─────────────────────────────────────────────────────────────
        // TECH — C
        // ─────────────────────────────────────────────────────────────
        seedCTech(tech, intern, allPos, cSet);
        seedCTech(tech, junior, allPos, cSet);
        seedCTech(tech, mid,    allPos, cSet);

        // ─────────────────────────────────────────────────────────────
        // CODE — Java
        // ─────────────────────────────────────────────────────────────
        seedJavaCode(problem, intern, allPos, javaSet);
        seedJavaCode(problem, junior, allPos, javaSet);
        seedJavaCode(problem, mid,    allPos, javaSet);
        seedJavaCode(problem, senior, allPos, javaSet);

        // ─────────────────────────────────────────────────────────────
        // CODE — Python
        // ─────────────────────────────────────────────────────────────
        seedPythonCode(problem, intern, allPos, pySet);
        seedPythonCode(problem, junior, allPos, pySet);
        seedPythonCode(problem, mid,    allPos, pySet);

        // ─────────────────────────────────────────────────────────────
        // CODE — JavaScript
        // ─────────────────────────────────────────────────────────────
        seedJsCode(problem, intern, allPos, jsSet);
        seedJsCode(problem, junior, allPos, jsSet);
        seedJsCode(problem, mid,    allPos, jsSet);

        // Test cases pentru întrebările CODE fără test cases
        addMissingTestCases();
    }

    // ═══════════════════════════════════════════════════════════════
    // HR
    // ═══════════════════════════════════════════════════════════════

    private void seedHR(QuestionCategory hr, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                text(hr, level, pos, lang, "Povestește-mi despre tine și de ce ai ales această carieră în IT.");
                text(hr, level, pos, lang, "Ce ai învățat până acum din proiecte personale sau academice?");
                text(hr, level, pos, lang, "Cum gestionezi situațiile în care nu știi cum să rezolvi o problemă?");
                text(hr, level, pos, lang, "De ce vrei să lucrezi la această companie?");
            }
            case "Junior" -> {
                text(hr, level, pos, lang, "Descrie un proiect la care ai lucrat și ce ai învățat din el.");
                text(hr, level, pos, lang, "Cum prioritizezi sarcinile când ai mai multe deadline-uri?");
                text(hr, level, pos, lang, "Descrie o situație în care ai primit feedback negativ și cum ai reacționat.");
                text(hr, level, pos, lang, "Cum lucrezi în echipă și cum gestionezi conflictele?");
                text(hr, level, pos, lang, "Ce înseamnă pentru tine un cod de calitate?");
            }
            case "Mid" -> {
                text(hr, level, pos, lang, "Descrie un proiect complex la care ai contribuit semnificativ.");
                text(hr, level, pos, lang, "Cum abordezi mentorarea colegilor mai puțin experimentați?");
                text(hr, level, pos, lang, "Cum iei decizii tehnice când există mai multe soluții posibile?");
                text(hr, level, pos, lang, "Descrie un moment în care ai depășit un obstacol tehnic major.");
                text(hr, level, pos, lang, "Cum echilibrezi viteza de livrare cu calitatea codului?");
            }
            case "Senior" -> {
                text(hr, level, pos, lang, "Cum ai influențat direcția tehnică a unei echipe sau organizații?");
                text(hr, level, pos, lang, "Descrie cea mai complexă decizie arhitecturală pe care ai luat-o.");
                text(hr, level, pos, lang, "Cum gestionezi dezacordurile tehnice cu alți seniori sau manageri?");
                text(hr, level, pos, lang, "Cum construiești o cultură inginerească sănătoasă în echipă?");
                text(hr, level, pos, lang, "Ce strategii folosești pentru a ține echipa motivată și productivă?");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TECH — Java
    // ═══════════════════════════════════════════════════════════════

    private void seedJavaTech(QuestionCategory tech, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                mcq(tech, level, pos, lang,
                        "[Java] Ce tip de date folosești pentru a stoca un număr întreg în Java?",
                        "float", "int", "char", "boolean", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce cuvânt cheie folosești pentru a crea o instanță a unei clase?",
                        "create", "new", "make", "instance", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Care este valoarea implicită a unui câmp int nedeclarat?",
                        "null", "1", "0", "-1", 'C');
                mcq(tech, level, pos, lang,
                        "[Java] Ce face metoda System.out.println()?",
                        "Citește de la tastatură", "Afișează text pe consolă", "Salvează date în fișier", "Aruncă o excepție", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce înseamnă că Java este un limbaj orientat pe obiecte?",
                        "Codul rulează mai rapid", "Programul este structurat în jurul obiectelor și claselor", "Nu are tipuri primitive", "Compilează în cod mașină direct", 'B');
            }
            case "Junior" -> {
                mcq(tech, level, pos, lang,
                        "[Java] Care structură de date garantează unicitatea elementelor?",
                        "ArrayList", "HashSet", "LinkedList", "ArrayDeque", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce se întâmplă dacă accesezi un obiect null?",
                        "Compilarea eșuează", "Returnează 0", "Apare NullPointerException la runtime", "Se creează automat obiectul", 'C');
                mcq(tech, level, pos, lang,
                        "[Java] Care este diferența dintre == și .equals() în Java?",
                        "Nu există diferență", "== compară referințele, .equals() compară conținutul", ".equals() compară referințele, == compară conținutul", "== funcționează doar pentru primitive", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce este o interfață în Java?",
                        "O clasă care nu poate fi instanțiată", "Un contract care definește metode fără implementare", "O clasă cu toate metodele statice", "Un tip de excepție", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce face cuvântul cheie final aplicat unei variabile?",
                        "O face privată", "Nu poate fi modificată după inițializare", "O face statică", "O șterge la final", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce este o excepție checked în Java?",
                        "O excepție care apare la compilare", "O excepție care trebuie gestionată sau declarată", "O excepție care nu poate fi capturată", "O excepție care oprește JVM-ul", 'B');
            }
            case "Mid" -> {
                mcq(tech, level, pos, lang,
                        "[Java] Ce este principiul SOLID — S (Single Responsibility)?",
                        "O clasă trebuie să fie singleton", "O clasă trebuie să aibă un singur motiv de schimbare", "O clasă trebuie să aibă o singură metodă", "O clasă nu poate extinde alta", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Când folosești HashMap vs TreeMap?",
                        "HashMap e mai lent", "HashMap oferă O(1) dar fără ordine; TreeMap e sortat O(log n)", "TreeMap e mai rapid pentru căutare", "Nu există diferență practică", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce este un design pattern Singleton?",
                        "O clasă cu un singur câmp", "Un pattern care asigură o singură instanță a clasei", "O clasă fără constructori", "Un pattern de moștenire multiplă", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce este Optional<T> în Java?",
                        "Un tip generic pentru colecții", "Un container care poate conține sau nu o valoare, evitând null", "O alternativă la ArrayList", "Un tip de excepție", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce face @Transactional în Spring?",
                        "Marchează o clasă ca singleton", "Asigură că operațiile DB sunt în aceeași tranzacție", "Optimizează query-urile SQL", "Validează datele de intrare", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce este Dependency Injection?",
                        "Un mod de a șterge dependențe", "Un pattern în care obiectele primesc dependențele din exterior", "O metodă de a crea obiecte fără new", "Un tip de moștenire", 'B');
            }
            case "Senior" -> {
                mcq(tech, level, pos, lang,
                        "[Java] Ce este CAP Theorem?",
                        "O regulă de compilare Java", "Un sistem distribuit nu poate garanta simultan Consistency, Availability și Partition tolerance", "Un pattern de design", "Un algoritm de sortare", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce este un Virtual Thread în Java 21?",
                        "Un thread care rulează în mașini virtuale", "Thread-uri ușoare gestionate de JVM, nu de OS", "Thread-uri pentru GUI", "Thread-uri care nu pot fi oprite", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Când alegi Kafka vs REST pentru comunicare între servicii?",
                        "Kafka e mai simplu", "Kafka pentru comunicare asincronă și event streaming; REST pentru sincron request-response", "REST e mai fiabil", "Nu există diferență", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce este Connection Pooling și de ce e important?",
                        "O metodă de criptare", "Reutilizarea conexiunilor DB pentru a evita overhead-ul de creare repetată", "Un tip de cache", "Un algoritm de load balancing", 'B');
                mcq(tech, level, pos, lang,
                        "[Java] Ce înseamnă eventual consistency?",
                        "Datele sunt întotdeauna consistente", "Sistemul garantează că, dat timp suficient, toate nodurile vor ajunge la aceeași stare", "Consistența este opțională", "Datele sunt consistente doar la scriere", 'B');
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TECH — Python
    // ═══════════════════════════════════════════════════════════════

    private void seedPythonTech(QuestionCategory tech, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                mcq(tech, level, pos, lang,
                        "[Python] Ce tip de date este imuabil în Python?",
                        "list", "dict", "tuple", "set", 'C');
                mcq(tech, level, pos, lang,
                        "[Python] Cum afișezi ceva pe consolă în Python?",
                        "console.log()", "System.out.println()", "print()", "echo()", 'C');
                mcq(tech, level, pos, lang,
                        "[Python] Ce face len() în Python?",
                        "Șterge o colecție", "Returnează lungimea unui obiect", "Creează o listă", "Sortează o colecție", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Cum creezi o listă în Python?",
                        "list = {1, 2, 3}", "list = [1, 2, 3]", "list = (1, 2, 3)", "list = <1, 2, 3>", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce este un modul în Python?",
                        "Un tip de variabilă", "Un fișier .py care conține cod reutilizabil", "O clasă specială", "Un tip de buclă", 'B');
            }
            case "Junior" -> {
                mcq(tech, level, pos, lang,
                        "[Python] Ce face list comprehension [x*2 for x in range(5)]?",
                        "Creează o liste cu primele 5 pătrate", "Creează lista [0, 2, 4, 6, 8]", "Creează lista [1, 2, 3, 4, 5]", "Returnează suma primelor 5 numere", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce este un decorator în Python?",
                        "O funcție care modifică altă funcție fără a schimba codul ei", "O clasă cu metode speciale", "Un tip de moștenire", "Un comentariu special", 'A');
                mcq(tech, level, pos, lang,
                        "[Python] Care este diferența dintre list și tuple?",
                        "Nu există diferență", "list e mutabilă, tuple e imutabilă", "tuple e mai lentă", "list nu acceptă numere", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce face *args într-o funcție Python?",
                        "Returnează toate variabilele", "Permite un număr variabil de argumente poziționale", "Multiplică argumentele", "Creează argumente opționale", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce este un generator în Python?",
                        "O funcție care creează o clasă", "O funcție care produce valori la cerere cu yield, eficientă cu memoria", "Un tip de buclă", "Un modul de generare de numere", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Cum gestionezi excepțiile în Python?",
                        "if/else", "try/except", "catch/throw", "error/handle", 'B');
            }
            case "Mid" -> {
                mcq(tech, level, pos, lang,
                        "[Python] Ce este GIL (Global Interpreter Lock)?",
                        "Un sistem de securitate Python", "Un mecanism care permite unui singur thread să execute cod Python la un moment dat", "Un garbage collector", "Un tip de lock pentru fișiere", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Când folosești asyncio în Python?",
                        "Pentru calcule matematice complexe", "Pentru operații I/O asincrone care nu blochează thread-ul principal", "Pentru sortare rapidă", "Pentru moștenire multiplă", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce face @property în Python?",
                        "Creează o proprietate statică", "Permite accesarea unei metode ca și cum ar fi un atribut", "Definește o proprietate privată", "Creează un decorator special", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce este duck typing în Python?",
                        "Un tip de moștenire", "Dacă un obiect are metodele necesare, poate fi folosit indiferent de tipul său", "Un sistem de tipare strictă", "Un pattern de design", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Care este complexitatea căutării într-un dict Python?",
                        "O(n)", "O(log n)", "O(1) amortizat", "O(n log n)", 'C');
            }
            case "Senior" -> {
                mcq(tech, level, pos, lang,
                        "[Python] Cum optimizezi memoria în Python pentru date mari?",
                        "Folosești mai multă RAM", "Folosești generators, itertools și structuri de date eficiente", "Folosești liste în loc de tuple", "Nu există optimizări", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce este metaclass în Python?",
                        "O clasă care nu poate fi instanțiată", "O clasă ale cărei instanțe sunt clase — controlează crearea claselor", "O clasă abstractă", "Un singleton", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Cum asiguri thread safety în Python?",
                        "GIL rezolvă totul automat", "Folosești threading.Lock, queue.Queue sau asyncio pentru diferite scenarii", "Folosești variabile globale", "Nu este posibil", 'B');
                mcq(tech, level, pos, lang,
                        "[Python] Ce este contextlib.contextmanager?",
                        "Un manager de pachete", "Un decorator pentru a crea context managers cu yield", "Un manager de memorie", "Un tip de decorator de clase", 'B');
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TECH — JavaScript
    // ═══════════════════════════════════════════════════════════════

    private void seedJsTech(QuestionCategory tech, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                mcq(tech, level, pos, lang,
                        "[JS] Ce face console.log() în JavaScript?",
                        "Salvează date în DB", "Afișează mesaje în consolă", "Trimite date la server", "Creează un fișier log", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Care este diferența dintre var și let?",
                        "Nu există diferență", "var are function scope, let are block scope", "let e mai rapid", "var nu mai există în JS modern", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce tip de date returnează typeof null?",
                        "\"null\"", "\"undefined\"", "\"object\"", "\"string\"", 'C');
                mcq(tech, level, pos, lang,
                        "[JS] Cum adaugi un element la sfârșitul unui array?",
                        "arr.add(elem)", "arr.push(elem)", "arr.append(elem)", "arr.insert(elem)", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce este un callback în JavaScript?",
                        "O funcție care apelează singură", "O funcție pasată ca argument și apelată ulterior", "Un tip de promisiune", "Un eveniment DOM", 'B');
            }
            case "Junior" -> {
                mcq(tech, level, pos, lang,
                        "[JS] Ce este o Promise în JavaScript?",
                        "Un tip de variabilă", "Un obiect care reprezintă rezultatul viitor al unei operații asincrone", "O funcție specială", "Un tip de callback", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce face Array.map() în JavaScript?",
                        "Caută un element", "Creează un nou array prin transformarea fiecărui element", "Sortează array-ul", "Filtrează elementele", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce este event bubbling?",
                        "Un tip de animație", "Propagarea evenimentului de la elementul copil spre părinte", "Un efect CSS", "Un tip de loop", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Diferența dintre == și === în JavaScript?",
                        "Nu există diferență", "== face type coercion, === compară tipul și valoarea", "=== e mai lent", "== funcționează doar cu numere", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce este closure în JavaScript?",
                        "O funcție care închide aplicația", "O funcție care reține accesul la variabilele din scope-ul exterior", "Un tip de moștenire", "O metodă de clonare", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce face async/await?",
                        "Blochează complet thread-ul", "Permite scrierea codului asincron într-un stil sincron, bazat pe Promise", "Creează noi thread-uri", "Anulează promisiunile", 'B');
            }
            case "Mid" -> {
                mcq(tech, level, pos, lang,
                        "[JS] Ce este the Event Loop în JavaScript?",
                        "Un tip de buclă for", "Mecanismul care gestionează execuția asincronă în JS single-threaded", "Un timer", "O buclă infinită", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce este prototypal inheritance?",
                        "Moștenire clasică ca în Java", "Obiectele moștenesc proprietăți direct de la alte obiecte prin lanțul prototype", "Un tip de mixin", "O clasă abstractă", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Când folosești useMemo în React?",
                        "Întotdeauna", "Pentru a memora rezultatul unui calcul costisitor și a evita recalcularea inutilă", "Doar pentru string-uri", "Pentru a accesa DOM-ul", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce este debouncing?",
                        "O optimizare CSS", "Amânarea execuției unei funcții până după ce un eveniment s-a oprit", "O metodă de sortare", "Un tip de caching", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce face Object.freeze()?",
                        "Copiază obiectul", "Împiedică modificarea, adăugarea sau ștergerea proprietăților", "Sortează proprietățile", "Serializează obiectul", 'B');
            }
            case "Senior" -> {
                mcq(tech, level, pos, lang,
                        "[JS] Ce este Web Workers API?",
                        "Un API pentru animații", "Permite rularea scripturilor JS în thread-uri separate, fără a bloca UI", "Un API pentru storage", "Un framework JS", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce este tree shaking în bundlere moderne?",
                        "O metodă de animație", "Eliminarea codului mort (nefolosit) din bundle-ul final", "O metodă de caching", "Un algoritm de sortare", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Diferența dintre microtask și macrotask în Event Loop?",
                        "Nu există diferență", "Microtask-urile (Promise, queueMicrotask) sunt procesate înaintea macrotask-urilor (setTimeout, setInterval)", "Macrotask-urile sunt prioritare", "Ambele se procesează simultan", 'B');
                mcq(tech, level, pos, lang,
                        "[JS] Ce este memoization și când o aplici?",
                        "Un tip de variabilă globală", "Caching-ul rezultatelor calculelor costisitoare pentru a evita recomputarea la aceleași inputuri", "Un design pattern pentru UI", "O metodă de compresie", 'B');
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TECH — C++
    // ═══════════════════════════════════════════════════════════════

    private void seedCppTech(QuestionCategory tech, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                mcq(tech, level, pos, lang,
                        "[C++] Ce este un pointer în C++?",
                        "Un tip de variabilă care stochează text", "O variabilă care stochează adresa de memorie a altei variabile", "O constantă", "Un tip de funcție", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce face operatorul & aplicat unei variabile?",
                        "Efectuează AND logic", "Returnează adresa de memorie a variabilei", "Copiază variabila", "Șterge variabila", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce este cout în C++?",
                        "O funcție matematică", "Obiectul standard de output pentru consolă", "Un tip de container", "O clasă abstractă", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce este diferența dintre struct și class în C++?",
                        "Nu există diferență", "Membrii struct sunt publici implicit, iar membrii class sunt privați implicit", "struct nu suportă metode", "class e mai rapid", 'B');
            }
            case "Junior" -> {
                mcq(tech, level, pos, lang,
                        "[C++] Ce este un destructor?",
                        "O funcție care creează obiecte", "O funcție apelată automat când un obiect iese din scope, pentru eliberarea resurselor", "Un operator special", "Un tip de constructor", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce este RAII?",
                        "Un tip de algoritm", "Resource Acquisition Is Initialization — resursele sunt legate de durata de viață a obiectelor", "Un design pattern de creație", "O optimizare a compilatorului", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce face std::vector față de un array C?",
                        "E mai lent", "Are dimensiune dinamică și gestionează automat memoria", "Nu suportă tipuri primitive", "E o listă înlănțuită", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce este smart pointer (unique_ptr)?",
                        "Un pointer mai rapid", "Un pointer care gestionează automat durata de viață a obiectului (ownership unic)", "Un pointer la funcții", "Un pointer constant", 'B');
            }
            case "Mid" -> {
                mcq(tech, level, pos, lang,
                        "[C++] Ce este move semantics?",
                        "Mutarea codului între fișiere", "Permite transferul resurselor fără copiere, folosind rvalue references", "Un algoritm de sorting", "O metodă de alocare", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce este undefined behavior?",
                        "O excepție standard", "Comportament al programului care nu este definit de standard — compilatorul poate face orice", "O eroare de compilare", "Un tip de warning", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Ce face constexpr?",
                        "Marchează o funcție ca privată", "Permite evaluarea expresiei la compile time", "Creează o constantă globală", "Dezactivează optimizările", 'B');
                mcq(tech, level, pos, lang,
                        "[C++] Diferența dintre std::map și std::unordered_map?",
                        "Nu există diferență", "map e sortat O(log n); unordered_map folosește hash table O(1) amortizat", "unordered_map e sortată", "map e mai rapid", 'B');
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TECH — C
    // ═══════════════════════════════════════════════════════════════

    private void seedCTech(QuestionCategory tech, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                mcq(tech, level, pos, lang,
                        "[C] Ce funcție folosești pentru a afișa pe consolă în C?",
                        "print()", "System.out.println()", "printf()", "cout <<", 'C');
                mcq(tech, level, pos, lang,
                        "[C] Ce este un pointer în C?",
                        "Un tip de variabilă care stochează numere mari", "O variabilă care stochează adresa de memorie", "O constantă", "Un tip de struct", 'B');
                mcq(tech, level, pos, lang,
                        "[C] Ce face malloc() în C?",
                        "Eliberează memorie", "Alocă dinamic un bloc de memorie și returnează pointerul", "Inițializează memorie cu 0", "Copiază memorie", 'B');
                mcq(tech, level, pos, lang,
                        "[C] Ce trebuie să faci după ce ai terminat cu memoria alocată cu malloc()?",
                        "Nimic, se eliberează automat", "Apelezi free()", "Apelezi delete()", "Resetezi pointerul la 0", 'B');
            }
            case "Junior" -> {
                mcq(tech, level, pos, lang,
                        "[C] Ce este un struct în C?",
                        "O funcție specială", "O colecție de variabile de tipuri diferite grupate împreună", "Un tip de pointer", "O constantă complexă", 'B');
                mcq(tech, level, pos, lang,
                        "[C] Ce face operatorul -> pentru pointeri la struct?",
                        "Compară două struct-uri", "Accesează un câmp al struct-ului printr-un pointer", "Avansează pointerul", "Șterge struct-ul", 'B');
                mcq(tech, level, pos, lang,
                        "[C] Ce este diferența dintre calloc() și malloc()?",
                        "Nu există diferență", "calloc() inițializează memoria cu 0, malloc() nu", "malloc() e mai rapid", "calloc() alocă mai multă memorie", 'B');
                mcq(tech, level, pos, lang,
                        "[C] Ce este o funcție recursivă?",
                        "O funcție care returnează void", "O funcție care se apelează pe sine, cu un caz de oprire", "O funcție cu mai mulți parametri", "O funcție din biblioteca standard", 'B');
            }
            case "Mid" -> {
                mcq(tech, level, pos, lang,
                        "[C] Ce este buffer overflow?",
                        "Un tip de excepție", "Scriere de date dincolo de limitele unui buffer, provocând corupere de memorie", "O eroare de compilare", "Un tip de warning al compilatorului", 'B');
                mcq(tech, level, pos, lang,
                        "[C] Ce este volatile în C?",
                        "O variabilă care nu poate fi modificată", "Indică că variabila poate fi modificată extern (hardware/OS), prevenind optimizările compilatorului", "O variabilă temporară", "Un tip de pointer", 'B');
                mcq(tech, level, pos, lang,
                        "[C] Ce face funcția memcpy()?",
                        "Compară două blocuri de memorie", "Copiază un bloc de memorie dintr-o locație în alta", "Alocă memorie", "Eliberează memorie", 'B');
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CODE — Java
    // ═══════════════════════════════════════════════════════════════

    private void seedJavaCode(QuestionCategory problem, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[Java] Calculează suma elementelor unui array de numere întregi.",
                        javaStarter("suma array", "sum"),
                        List.of(
                                tc("Array simplu",   "5\n1 2 3 4 5",  "15"),
                                tc("Numere negative","3\n-1 -2 -3",   "-6"),
                                tc("Un element",     "1\n42",         "42")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[Java] Numără vocalele dintr-un string (a, e, i, o, u — litere mici și mari).",
                        javaStarter("vocale", "countVowels"),
                        List.of(
                                tc("Cuvânt simplu", "hello",  "2"),
                                tc("Doar vocale",   "aeiou",  "5"),
                                tc("Fără vocale",   "xyz",    "0"),
                                tc("Litere mari",   "Hello",  "2")
                        ));
            }
            case "Junior" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[Java] Verifică dacă un string este palindrom (ignoră majusculele).",
                        javaStarter("palindrom", "isPalindrome"),
                        List.of(
                                tc("Palindrom simplu",    "ana",   "true"),
                                tc("Nu e palindrom",      "mere",  "false"),
                                tc("Un singur caracter",  "a",     "true"),
                                tc("Cu litere mari",      "Ana",   "true")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[Java] Generează primele N numere Fibonacci (separate prin spațiu).",
                        javaStarter("fibonacci", "fibonacci"),
                        List.of(
                                tc("Primele 5",  "5", "0 1 1 2 3"),
                                tc("Primul",     "1", "0"),
                                tc("Primele 8",  "8", "0 1 1 2 3 5 8 13")
                        ));
            }
            case "Mid" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[Java] Verifică dacă un număr întreg este număr prim.",
                        javaStarter("prim", "isPrime"),
                        List.of(
                                tc("Număr prim",       "7",  "true"),
                                tc("Nu e prim",        "4",  "false"),
                                tc("1 nu e prim",      "1",  "false"),
                                tc("2 e prim",         "2",  "true"),
                                tc("Număr mare prim",  "13", "true")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[Java] Inversează cuvintele dintr-o propoziție (cuvintele în ordine inversă).",
                        javaStarter("inverseaza cuvinte", "reverseWords"),
                        List.of(
                                tc("Două cuvinte",   "hello world",    "world hello"),
                                tc("Trei cuvinte",   "Java este bun",  "bun este Java"),
                                tc("Un cuvânt",      "salut",          "salut")
                        ));
            }
            case "Senior" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[Java] Găsește perechile de numere dintr-un array care au suma egală cu un target dat. Prima linie: N și target. A doua linie: N numere.",
                        javaStarter("perechi suma", "findPairs"),
                        List.of(
                                tc("Perechi simple",    "5 5\n1 2 3 4 5",     "(1,4) (2,3)"),
                                tc("Nicio pereche",     "4 20\n1 2 3 4",      ""),
                                tc("O pereche",         "4 7\n1 2 5 6",       "(1,6) (2,5)")
                        ));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CODE — Python
    // ═══════════════════════════════════════════════════════════════

    private void seedPythonCode(QuestionCategory problem, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[Python] Calculează suma elementelor unui array. Prima linie: N. A doua linie: numerele.",
                        pythonStarter("suma array"),
                        List.of(
                                tc("Array simplu",    "5\n1 2 3 4 5",  "15"),
                                tc("Numere negative", "3\n-1 -2 -3",   "-6"),
                                tc("Un element",      "1\n42",         "42")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[Python] Numără vocalele dintr-un string (a, e, i, o, u — litere mici și mari).",
                        pythonStarter("numaratoare vocale"),
                        List.of(
                                tc("Cuvânt simplu", "hello",  "2"),
                                tc("Doar vocale",   "aeiou",  "5"),
                                tc("Fără vocale",   "xyz",    "0")
                        ));
            }
            case "Junior" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[Python] Verifică dacă un string este palindrom (ignoră majusculele). Afișează true sau false.",
                        pythonStarter("palindrom verificare"),
                        List.of(
                                tc("Palindrom",      "ana",   "true"),
                                tc("Nu e palindrom", "mere",  "false"),
                                tc("Un caracter",    "a",     "true")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[Python] Verifică dacă un număr este prim. Afișează true sau false.",
                        pythonStarter("verificare numar prim"),
                        List.of(
                                tc("Prim",    "7", "true"),
                                tc("Nu e",    "4", "false"),
                                tc("1",       "1", "false"),
                                tc("2",       "2", "true")
                        ));
            }
            case "Mid" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[Python] Inversează cuvintele dintr-o propoziție.",
                        pythonStarter("inversare cuvinte"),
                        List.of(
                                tc("Două cuvinte", "hello world",   "world hello"),
                                tc("Trei cuvinte", "Python e bun",  "bun e Python")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[Python] Găsește cel mai frecvent element dintr-o listă. Prima linie: N. A doua linie: N numere.",
                        pythonStarter("element frecvent"),
                        List.of(
                                tc("Cel mai frecvent", "6\n1 2 2 3 3 3", "3"),
                                tc("Toate unice",      "3\n1 2 3",       "1"),
                                tc("Un element",       "1\n5",           "5")
                        ));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CODE — JavaScript
    // ═══════════════════════════════════════════════════════════════

    private void seedJsCode(QuestionCategory problem, Level level, Set<Position> pos, Set<Language> lang) {
        String lvl = level.getName();
        switch (lvl) {
            case "Intern" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[JS] Calculează suma elementelor unui array. Prima linie: N. A doua linie: numerele.",
                        jsStarter("suma array"),
                        List.of(
                                tc("Array simplu",    "5\n1 2 3 4 5",  "15"),
                                tc("Numere negative", "3\n-1 -2 -3",   "-6"),
                                tc("Un element",      "1\n42",         "42")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[JS] Numără vocalele dintr-un string (a, e, i, o, u — litere mici și mari).",
                        jsStarter("vocale"),
                        List.of(
                                tc("Cuvânt simplu", "hello",  "2"),
                                tc("Doar vocale",   "aeiou",  "5"),
                                tc("Fără vocale",   "xyz",    "0")
                        ));
            }
            case "Junior" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[JS] Verifică dacă un string este palindrom (ignoră majusculele). Afișează true sau false.",
                        jsStarter("palindrom"),
                        List.of(
                                tc("Palindrom",      "ana",   "true"),
                                tc("Nu e palindrom", "mere",  "false"),
                                tc("Un caracter",    "a",     "true")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[JS] Calculează factorialul unui număr N (N citit din stdin).",
                        jsStarter("factorial"),
                        List.of(
                                tc("5! = 120",  "5",  "120"),
                                tc("0! = 1",    "0",  "1"),
                                tc("1! = 1",    "1",  "1"),
                                tc("6! = 720",  "6",  "720")
                        ));
            }
            case "Mid" -> {
                saveCodeWithTests(problem, level, pos, lang,
                        "[JS] Inversează cuvintele dintr-o propoziție.",
                        jsStarter("inversare cuvinte"),
                        List.of(
                                tc("Două cuvinte", "hello world",   "world hello"),
                                tc("Trei cuvinte", "JS este bun",   "bun este JS")
                        ));
                saveCodeWithTests(problem, level, pos, lang,
                        "[JS] Verifică dacă un număr este prim. Afișează true sau false.",
                        jsStarter("numar prim"),
                        List.of(
                                tc("Prim",  "7",  "true"),
                                tc("Nu e",  "4",  "false"),
                                tc("2",     "2",  "true"),
                                tc("1",     "1",  "false")
                        ));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Test cases pentru întrebările CODE existente fără test cases
    // ═══════════════════════════════════════════════════════════════

    private void addMissingTestCases() {
        questionRepo.findAll().stream()
                .filter(q -> "CODE".equals(q.getAnswerType()))
                .filter(q -> !testCaseRepo.existsByQuestionId(q.getId()))
                .forEach(q -> {
                    String t = q.getText().toLowerCase();
                    if (t.contains("palindrom")) {
                        tc(q, "Palindrom simplu",   "ana",   "true",  1);
                        tc(q, "Nu e palindrom",     "mere",  "false", 2);
                        tc(q, "Un caracter",        "a",     "true",  3);
                    } else if (t.contains("perech") || t.contains("suma")) {
                        tc(q, "Test simplu",    "5\n1 2 3 4 5",  "15", 1);
                        tc(q, "Negativ",        "3\n-1 -2 -3",  "-6", 2);
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════════
    // Starter code templates
    // ═══════════════════════════════════════════════════════════════

    private String javaStarter(String hint, String funcName) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        // Citește datele din stdin și afișează rezultatul cu System.out.print()
                    }
                }
                """;
    }

    private String pythonStarter(String hint) {
        return """
                import sys

                def solve():
                    # Citește datele și afișează rezultatul cu print()
                    pass

                solve()
                """;
    }

    private String jsStarter(String hint) {
        return """
                const lines = require('fs').readFileSync('/dev/stdin', 'utf8').trim().split('\\n');
                let idx = 0;

                // Procesează liniile și afișează rezultatul cu process.stdout.write()
                """;
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers — creare entități
    // ═══════════════════════════════════════════════════════════════

    private void text(QuestionCategory cat, Level level, Set<Position> pos, Set<Language> lang, String questionText) {
        if (existingTexts.contains(questionText)) return;
        Question q = new Question();
        q.setCategory(cat);
        q.setLevel(level);
        q.setText(questionText);
        q.setAnswerType("TEXT");
        q.setActive(true);
        q.setPositions(new HashSet<>(pos));
        q.setLanguages(new HashSet<>(lang));
        questionRepo.save(q);
        existingTexts.add(questionText);
    }

    private void mcq(QuestionCategory cat, Level level, Set<Position> pos, Set<Language> lang,
                     String questionText, String a, String b, String c, String d, char correct) {
        if (existingTexts.contains(questionText)) return;
        Question q = new Question();
        q.setCategory(cat);
        q.setLevel(level);
        q.setText(questionText);
        q.setAnswerType("MCQ");
        q.setActive(true);
        q.setPositions(new HashSet<>(pos));
        q.setLanguages(new HashSet<>(lang));
        q.getOptions().add(new QuestionOption(q, a, correct == 'A'));
        q.getOptions().add(new QuestionOption(q, b, correct == 'B'));
        q.getOptions().add(new QuestionOption(q, c, correct == 'C'));
        q.getOptions().add(new QuestionOption(q, d, correct == 'D'));
        questionRepo.save(q);
        existingTexts.add(questionText);
    }

    private void saveCodeWithTests(QuestionCategory cat, Level level, Set<Position> pos, Set<Language> lang,
                                   String questionText, String starterCode, List<String[]> testCases) {
        if (existingTexts.contains(questionText)) return;
        Question q = new Question();
        q.setCategory(cat);
        q.setLevel(level);
        q.setText(questionText);
        q.setAnswerType("CODE");
        q.setStarterCode(starterCode);
        q.setActive(true);
        q.setPositions(new HashSet<>(pos));
        q.setLanguages(new HashSet<>(lang));
        Question saved = questionRepo.save(q);
        existingTexts.add(questionText);

        for (int i = 0; i < testCases.size(); i++) {
            String[] row = testCases.get(i);
            tc(saved, row[0], row[1], row[2], i + 1);
        }
    }

    private String[] tc(String desc, String input, String expected) {
        return new String[]{desc, input, expected};
    }

    private void tc(Question q, String desc, String input, String expected, int order) {
        TestCase t = new TestCase();
        t.setQuestion(q);
        t.setDescription(desc);
        t.setInputData(input.isEmpty() ? null : input);
        t.setExpectedOutput(expected);
        t.setOrderIndex(order);
        testCaseRepo.save(t);
    }

    private Set<Language> buildLangSet(Language... langs) {
        Set<Language> s = new HashSet<>();
        for (Language l : langs) if (l != null) s.add(l);
        return s;
    }

    private QuestionCategory cat(String name) {
        return categoryRepo.findByName(name)
                .orElseGet(() -> categoryRepo.save(new QuestionCategory(name)));
    }

    private Level level(String name) {
        return levelRepo.findAll().stream()
                .filter(l -> l.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private Position pos(String name) {
        return positionRepo.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private Language lang(String name) {
        return languageRepo.findAll().stream()
                .filter(x -> x.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
}
