package com.licenta.backend.interviews.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.licenta.backend.interviews.api.dto.AnswerFeedbackResultDto;
import com.licenta.backend.interviews.api.dto.InterviewOverviewResultDto;
import com.licenta.backend.interviews.core.entity.AnswerFeedback;
import com.licenta.backend.interviews.core.entity.InterviewAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(AiFeedbackService.class);

    private final GroqApiClient groqClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiFeedbackService(GroqApiClient groqClient) {
        this.groqClient = groqClient;
    }

    // ─── Per-answer feedback ──────────────────────────────────────────────────

    public AnswerFeedbackResultDto analyzeAnswer(InterviewAnswer answer) {
        String questionText = answer.getInterviewQuestion().getQuestion().getText();
        String answerType   = answer.getInterviewQuestion().getQuestion().getAnswerType();
        String userAnswer   = answer.getAnswerText() == null ? "" : answer.getAnswerText().trim();
        List<String> languages = answer.getInterviewQuestion().getQuestion().getLanguages()
                .stream().map(l -> l.getName()).toList();

        if (userAnswer.isBlank()) {
            return blankAnswerResult();
        }

        if ("CODE".equalsIgnoreCase(answerType)) {
            return analyzeCodingAnswer(questionText, userAnswer, languages);
        } else {
            return analyzeHrAnswer(questionText, userAnswer);
        }
    }

    // ─── HR / TEXT feedback ───────────────────────────────────────────────────

    private AnswerFeedbackResultDto analyzeHrAnswer(String question, String answer) {
        try {
            String prompt      = buildHrPromptV2(question, answer);
            String rawResponse = groqClient.generate(prompt);
            String json        = extractJson(rawResponse);
            JsonNode hr = objectMapper.readTree(json);

            AnswerFeedbackResultDto result = new AnswerFeedbackResultDto();
            result.score           = intField(hr, "score", 5);
            result.good            = boolField(hr, "good", result.score >= 6);
            result.strengths       = textField(hr, "strongPoints");
            result.weaknesses      = textField(hr, "missingPoints");
            result.improvementTips = textField(hr, "improvementTips");
            result.suggestedAnswer = textField(hr, "tailoredExampleAnswer");
            result.feedbackJson    = objectMapper.writeValueAsString(hr);

            log.info("HR feedback OK. score={}", result.score);
            return result;

        } catch (Exception e) {
            log.error("HR analyzeAnswer failed: {}", e.getMessage(), e);
            return fallbackAnswerResult(e);
        }
    }

    // ─── Coding feedback ──────────────────────────────────────────────────────

    private AnswerFeedbackResultDto analyzeCodingAnswer(String question, String answer, List<String> languages) {
        try {
            String lang        = languages.isEmpty() ? "necunoscut" : String.join(", ", languages);
            String prompt      = buildCodingPromptV2(question, answer, lang);
            String rawResponse = groqClient.generate(prompt);
            String json        = extractJson(rawResponse);
            JsonNode coding = objectMapper.readTree(json);

            AnswerFeedbackResultDto result = new AnswerFeedbackResultDto();
            result.score           = intField(coding, "score", 5);
            result.good            = boolField(coding, "good", result.score >= 6);
            result.strengths       = textField(coding, "strengths");
            result.weaknesses      = textField(coding, "weaknesses");
            result.improvementTips = textField(coding, "improvementTips");
            result.suggestedAnswer = textField(coding, "modelSolution");
            result.feedbackJson    = objectMapper.writeValueAsString(coding);

            log.info("Coding feedback OK. score={}", result.score);
            return result;

        } catch (Exception e) {
            log.error("Coding analyzeAnswer failed: {}", e.getMessage(), e);
            return fallbackAnswerResult(e);
        }
    }

    // ─── Overall interview overview ───────────────────────────────────────────

    public InterviewOverviewResultDto analyzeInterviewOverview(int mcqScore, List<AnswerFeedback> feedbackList) {
        try {
            String prompt      = buildOverviewPrompt(mcqScore, feedbackList);
            String rawResponse = groqClient.generate(prompt);
            String json        = extractJson(rawResponse);
            InterviewOverviewResultDto result = objectMapper.readValue(json, InterviewOverviewResultDto.class);
            log.info("Overview feedback OK. readyLevel={}", result.readyLevel);
            return result;
        } catch (Exception e) {
            log.error("analyzeInterviewOverview failed: {}", e.getMessage(), e);
            return fallbackOverview(mcqScore);
        }
    }

    // ─── Prompt builders ─────────────────────────────────────────────────────

    private String buildHrPromptV2(String question, String answer) {
        return """
                Esti un interview coach expert. Analizeaza raspunsul candidatului la aceasta intrebare de interviu.
                Returneaza EXCLUSIV un obiect JSON valid. Fara markdown, fara ``` si fara text suplimentar.
                Nu copia valorile exemplu. Inlocuieste fiecare valoare cu evaluarea ta concreta.

                Intrebare: %s
                Raspunsul candidatului: %s

                Returneaza exact aceasta structura JSON valida:
                {
                  "score": 5,
                  "good": false,
                  "matchedPoints": "aspectele bune atinse de raspuns",
                  "strongPoints": "ce este bine formulat sau valoros in raspuns",
                  "problematicMentions": "formulari riscante, vagi sau neprofesionale; daca nu exista, scrie Niciuna identificata",
                  "missingPoints": "ce lipseste fata de ce ar astepta un recrutor",
                  "improvementTips": "3-5 sfaturi concrete si aplicabile",
                  "idealAnswerStructure": "structura ideala a raspunsului",
                  "tailoredExampleAnswer": "un exemplu concret de raspuns mai bun, adaptat candidatului",
                  "toneFeedback": "feedback despre ton, claritate si profesionalism",
                  "finalVerdict": "concluzie clara in 2-3 propozitii"
                }

                Criterii: relevanta, claritate, specificitate, structura, exemple, maturitate profesionala si red flags.
                """.formatted(question, answer);
    }

    private String buildCodingPromptV2(String question, String answer, String language) {
        return """
                Esti un evaluator tehnic expert. Analizeaza solutia de programare a candidatului.
                Returneaza EXCLUSIV un obiect JSON valid. Fara markdown, fara ``` si fara text suplimentar.
                Nu copia valorile exemplu. Inlocuieste fiecare valoare cu evaluarea ta concreta.
                Pentru campurile de cod, foloseste \\n pentru newline si spatii pentru indentare.

                Problema: %s
                Limbaj: %s
                Solutia candidatului:
                %s

                Returneaza exact aceasta structura JSON valida:
                {
                  "score": 5,
                  "good": false,
                  "problemUnderstanding": "daca a inteles cerinta si cum a abordat-o",
                  "correctness": "cat de corecta este solutia si de ce",
                  "codeRuns": "unknown",
                  "runIssues": "daca nu ruleaza, explica exact de ce; daca ruleaza, scrie N/A",
                  "logicIssues": "probleme de logica identificate; daca nu exista, scrie Niciuna",
                  "syntaxIssues": "probleme de sintaxa; daca nu exista, scrie Niciuna",
                  "edgeCases": "cazuri limita netratate",
                  "timeComplexity": "complexitatea timp estimata cu explicatie scurta",
                  "spaceComplexity": "complexitatea spatiu estimata cu explicatie scurta",
                  "goodPracticesUsed": "bune practici folosite corect",
                  "badPracticesUsed": "practici proaste sau riscante",
                  "testedExamples": "2-3 exemple concrete cu observatii",
                  "strengths": "punctele forte ale solutiei candidatului",
                  "weaknesses": "punctele slabe principale",
                  "improvementTips": "3-5 sfaturi concrete",
                  "correctedCode": "versiune corectata sau N/A",
                  "modelSolution": "solutie model curata si corecta",
                  "explanationOfSolution": "explicatie clara a solutiei model",
                  "finalVerdict": "concluzie finala"
                }
                """.formatted(question, language, answer);
    }

    private String buildHrPrompt(String question, String answer) {
        return """
                Ești un interview coach expert. Analizează răspunsul candidatului la această întrebare de interviu.
                Returnează EXCLUSIV un obiect JSON valid. Fără markdown, fără ``` , fără text suplimentar.

                Întrebare: %s
                Răspunsul candidatului: %s

                Returnează exact această schemă JSON (toate câmpurile obligatorii, valori string):
                {
                  "score": <număr întreg 0-10>,
                  "good": <true dacă score >= 6, altfel false>,
                  "matchedPoints": "<ce aspecte bune a atins răspunsul>",
                  "strongPoints": "<ce este bine formulat sau valoros în răspuns>",
                  "problematicMentions": "<ce a spus greșit, formulări riscante, vagi, prea negative sau neprofesionale — dacă nu există, scrie 'Niciuna identificată'>",
                  "missingPoints": "<ce lipsește din răspuns față de ce ar aștepta un recrutor>",
                  "improvementTips": "<3-5 sfaturi concrete și aplicabile pentru a îmbunătăți răspunsul>",
                  "idealAnswerStructure": "<structura ideală pentru un răspuns la această întrebare: ce secțiuni, ce informații, ce ordine>",
                  "tailoredExampleAnswer": "<un exemplu concret de răspuns mai bun, adaptat la ideea inițială a candidatului, gata de folosit>",
                  "toneFeedback": "<feedback despre ton, claritate, naturalețe, nivel de detaliu, profesionalism>",
                  "finalVerdict": "<concluzie clară și utilă în 2-3 propoziții>"
                }

                Criterii de evaluare:
                - Relevanța față de întrebare (0-10 nu înseamnă dezastru/perfect — fii calibrat)
                - Claritate și specificitate
                - Structurarea ideilor
                - Nivelul de detaliu și exemplele oferite
                - Naturalețea și maturitatea profesională
                - Prezența sau absența red flag-urilor
                - Potrivirea pentru un context real de interviu
                """.formatted(question, answer);
    }

    private String buildCodingPrompt(String question, String answer, String language) {
        return """
                Ești un evaluator tehnic expert. Analizează soluția de programare a candidatului.
                Returnează EXCLUSIV un obiect JSON valid. Fără markdown, fără ``` , fără text suplimentar.
                Pentru câmpurile de cod (correctedCode, modelSolution), folosește \\n pentru newline și \\t sau spații pentru indentare.

                Problema: %s
                Limbaj: %s
                Soluția candidatului:
                %s

                Returnează exact această schemă JSON (toate câmpurile obligatorii):
                {
                  "score": <număr întreg 0-10>,
                  "good": <true dacă score >= 6, altfel false>,
                  "problemUnderstanding": "<dacă candidatul a înțeles cerința și cum a abordat-o>",
                  "correctness": "<cât de corectă este soluția și de ce>",
                  "codeRuns": "<true / false / unknown>",
                  "runIssues": "<dacă nu rulează, explică exact de ce — dacă rulează, scrie 'N/A'>",
                  "logicIssues": "<probleme de logică identificate — dacă nu există, scrie 'Niciuna'>",
                  "syntaxIssues": "<probleme de sintaxă — dacă nu există, scrie 'Niciuna'>",
                  "edgeCases": "<cazuri limită netratate: input gol, null, numere negative, overflow etc.>",
                  "timeComplexity": "<complexitatea timp estimată cu explicație scurtă>",
                  "spaceComplexity": "<complexitatea spațiu estimată cu explicație scurtă>",
                  "goodPracticesUsed": "<bune practici folosite corect — dacă nu există, scrie 'Niciuna'>",
                  "badPracticesUsed": "<practici proaste sau riscante — dacă nu există, scrie 'Niciuna'>",
                  "testedExamples": "<2-3 exemple concrete: input → output așteptat + ce returnează soluția candidatului + observații>",
                  "strengths": "<punctele forte ale soluției candidatului>",
                  "weaknesses": "<punctele slabe principale>",
                  "improvementTips": "<3-5 sfaturi concrete pentru a îmbunătăți soluția>",
                  "correctedCode": "<dacă soluția candidatului e aproape corectă, întoarce o versiune corectată a CODULUI LUI cu \\n și indentare — dacă e complet greșită, scrie 'N/A'>",
                  "modelSolution": "<o soluție model curată, corectă și eficientă, cu \\n și indentare>",
                  "explanationOfSolution": "<explicație clară a soluției model: cum funcționează, de ce e optimă>",
                  "finalVerdict": "<concluzie finală: ce a arătat candidatul, ce trebuie să îmbunătățească>"
                }
                """.formatted(question, language, answer);
    }

    private String buildOverviewPrompt(int mcqScore, List<AnswerFeedback> feedbackList) {
        StringBuilder answersBuilder = new StringBuilder();
        for (int i = 0; i < feedbackList.size(); i++) {
            AnswerFeedback f = feedbackList.get(i);
            String questionText = f.getInterviewAnswer().getInterviewQuestion().getQuestion().getText();
            String answerText   = f.getInterviewAnswer().getAnswerText();
            String answerType   = f.getInterviewAnswer().getInterviewQuestion().getQuestion().getAnswerType();
            int score           = f.getScore() != null ? f.getScore() : 0;

            answersBuilder.append("Răspuns ").append(i + 1)
                    .append(" [").append(answerType).append("] (scor AI: ").append(score).append("/10)\n");
            answersBuilder.append("Întrebare: ").append(questionText).append("\n");
            answersBuilder.append("Răspuns: ").append(answerText != null ? answerText : "(fără răspuns)").append("\n\n");
        }

        return """
                Ești un evaluator AI de interviuri tehnice. Oferă o evaluare generală a performanței candidatului.
                Returnează EXCLUSIV un obiect JSON valid. Fără markdown, fără ``` , fără text suplimentar.

                Scor grile (MCQ): %d%%
                Răspunsuri deschise analizate:
                %s

                Returnează exact această schemă JSON (toate câmpurile obligatorii, valori string):
                {
                  "readyLevel": "<READY | PARTIALLY_READY | NOT_READY>",
                  "summaryText": "<rezumat general al performanței candidatului în 3-5 propoziții>",
                  "strongPoints": "<punctele forte principale demonstrate în interviu>",
                  "weakPoints": "<punctele slabe și lacunele identificate>",
                  "recommendedTopics": "<subiecte concrete de studiat, prioritizate>",
                  "recommendedProblemCategories": "<categorii de probleme de exersat pe leetcode/codeforces>",
                  "nextSteps": "<3-5 pași concreți și acționabili pentru a avansa>"
                }

                Criterii readyLevel:
                - READY: scor MCQ >= 70%% și răspunsuri deschise clare și solide
                - PARTIALLY_READY: bază decentă dar cu lacune clare sau inconsistente
                - NOT_READY: răspunsuri slabe, incomplete sau lipsă la majoritatea întrebărilor
                """.formatted(mcqScore, answersBuilder);
    }

    // ─── JSON extraction ─────────────────────────────────────────────────────

    private String extractJson(String text) {
        if (text == null || text.isBlank()) return "{}";

        String clean = text
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int start = clean.indexOf('{');
        int end   = clean.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return clean.substring(start, end + 1);
        }
        return clean;
    }

    // ─── Fallbacks ───────────────────────────────────────────────────────────

    private int intField(JsonNode node, String fieldName, int fallback) {
        JsonNode field = node.get(fieldName);
        return field != null && field.canConvertToInt() ? field.asInt() : fallback;
    }

    private boolean boolField(JsonNode node, String fieldName, boolean fallback) {
        JsonNode field = node.get(fieldName);
        return field != null && field.isBoolean() ? field.asBoolean() : fallback;
    }

    private String textField(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return "";
        }
        if (field.isTextual() || field.isNumber() || field.isBoolean()) {
            return field.asText();
        }
        if (field.isArray()) {
            java.util.List<String> values = new java.util.ArrayList<>();
            field.forEach(item -> values.add(item.isValueNode() ? item.asText() : item.toString()));
            return String.join("\n", values);
        }
        return field.toString();
    }

    private AnswerFeedbackResultDto blankAnswerResult() {
        AnswerFeedbackResultDto r = new AnswerFeedbackResultDto();
        r.score           = 0;
        r.good            = false;
        r.strengths       = "Nu există puncte forte — răspunsul lipsește.";
        r.weaknesses      = "Nu ai oferit niciun răspuns.";
        r.improvementTips = "Formulează un răspuns complet și relevant.";
        r.suggestedAnswer = "Construiește un răspuns care să atingă ideile principale.";
        r.feedbackJson    = null;
        return r;
    }

    private AnswerFeedbackResultDto fallbackAnswerResult(Exception cause) {
        AnswerFeedbackResultDto r = new AnswerFeedbackResultDto();
        r.score           = 5;
        r.good            = false;
        r.strengths       = "Răspuns primit.";
        r.weaknesses      = "Feedback AI indisponibil momentan.";
        r.improvementTips = "Încearcă să reiei interviul mai târziu.";
        r.suggestedAnswer = "—";
        r.feedbackJson    = "{\"fallback\":true,\"reason\":\"" + sanitizeReason(cause) + "\"}";
        return r;
    }

    private String sanitizeReason(Exception cause) {
        if (cause == null || cause.getMessage() == null) {
            return "unknown";
        }
        return cause.getMessage()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private InterviewOverviewResultDto fallbackOverview(int mcqScore) {
        InterviewOverviewResultDto r = new InterviewOverviewResultDto();
        r.readyLevel                   = mcqScore >= 75 ? "READY" : mcqScore >= 45 ? "PARTIALLY_READY" : "NOT_READY";
        r.summaryText                  = "Evaluarea AI nu este disponibilă momentan.";
        r.strongPoints                 = "—";
        r.weakPoints                   = "—";
        r.recommendedTopics            = "—";
        r.recommendedProblemCategories = "—";
        r.nextSteps                    = "Încearcă din nou mai târziu.";
        return r;
    }
}
