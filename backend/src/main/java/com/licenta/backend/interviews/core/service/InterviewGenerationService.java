package com.licenta.backend.interviews.core.service;

import com.licenta.backend.interviews.api.dto.InterviewDetailsResponseDto;
import com.licenta.backend.interviews.api.dto.InterviewGenerateRequestDto;
import com.licenta.backend.interviews.core.entity.Interview;
import com.licenta.backend.interviews.core.entity.InterviewQuestion;
import com.licenta.backend.interviews.core.entity.InterviewRequest;
import com.licenta.backend.interviews.core.repo.InterviewAnswerRepo;
import com.licenta.backend.interviews.core.repo.InterviewQuestionRepo;
import com.licenta.backend.interviews.core.repo.InterviewRepo;
import com.licenta.backend.interviews.core.repo.InterviewRequestRepo;
import com.licenta.backend.interviews.filters.repo.LanguageRepository;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import com.licenta.backend.questions.entity.Question;
import com.licenta.backend.questions.repo.QuestionCategoryRepo;
import com.licenta.backend.questions.repo.QuestionOptionRepo;
import com.licenta.backend.questions.repo.QuestionRepo;
import com.licenta.backend.users.entity.User;
import com.licenta.backend.users.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.licenta.backend.interviews.api.dto.SubmitInterviewAnswersRequestDto;
import com.licenta.backend.interviews.core.entity.InterviewAnswer;
import java.util.HashSet;
import com.licenta.backend.interviews.api.dto.InterviewResultDto;
import java.util.List;
import com.licenta.backend.interviews.api.dto.InterviewHistoryItemDto;
import com.licenta.backend.interviews.api.dto.InterviewReviewResponseDto;
import com.licenta.backend.interviews.core.entity.AnswerFeedback;
import com.licenta.backend.interviews.core.repo.AnswerFeedbackRepo;
import com.licenta.backend.interviews.api.dto.InterviewOverviewResultDto;
import com.licenta.backend.interviews.core.entity.InterviewFeedbackSummary;
import com.licenta.backend.interviews.core.repo.InterviewFeedbackSummaryRepo;

@Service
public class InterviewGenerationService {

    private final InterviewRequestRepo requestRepo;
    private final InterviewRepo interviewRepo;
    private final UserRepo userRepo;

    private final AnswerFeedbackRepo answerFeedbackRepo;

    private final AiFeedbackService aiFeedbackService;
    private final LevelRepository levelRepo;
    private final PositionRepository positionRepo;
    private final LanguageRepository languageRepo;
    private final QuestionRepo questionRepo;
    private final QuestionCategoryRepo categoryRepo;
    private final InterviewQuestionRepo interviewQuestionRepo;
    private final InterviewAnswerRepo interviewAnswerRepo;
    private final QuestionOptionRepo questionOptionRepo;
    private final InterviewFeedbackSummaryRepo interviewFeedbackSummaryRepo;

    public InterviewGenerationService(InterviewRequestRepo requestRepo,
                                      InterviewRepo interviewRepo,
                                      UserRepo userRepo,
                                      LevelRepository levelRepo,
                                      PositionRepository positionRepo,
                                      QuestionRepo questionRepo,
                                      QuestionCategoryRepo categoryRepo,
                                      InterviewQuestionRepo interviewQuestionRepo,
                                      InterviewAnswerRepo interviewAnswerRepo,
                                      QuestionOptionRepo questionOptionRepo,
                                      LanguageRepository languageRepo,
                                      AnswerFeedbackRepo answerFeedbackRepo,
                                      AiFeedbackService aiFeedbackService,
                                      InterviewFeedbackSummaryRepo interviewFeedbackSummaryRepo) {
        this.requestRepo = requestRepo;
        this.interviewRepo = interviewRepo;
        this.userRepo = userRepo;
        this.levelRepo = levelRepo;
        this.positionRepo = positionRepo;
        this.languageRepo = languageRepo;
        this.questionRepo = questionRepo;
        this.categoryRepo = categoryRepo;
        this.interviewQuestionRepo = interviewQuestionRepo;
        this.interviewAnswerRepo = interviewAnswerRepo;
        this.questionOptionRepo = questionOptionRepo;
        this.answerFeedbackRepo = answerFeedbackRepo;
        this.aiFeedbackService = aiFeedbackService;
        this.interviewFeedbackSummaryRepo = interviewFeedbackSummaryRepo;
    }

    @Transactional
    public Interview generate(InterviewGenerateRequestDto req) {
        if (req.levelId == null || req.positionId == null || req.languageIds == null || req.languageIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selectează level, position și cel puțin un limbaj.");
        }

        User currentUser = getCurrentUser();

        var level = levelRepo.findById(req.levelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Level invalid"));
        var position = positionRepo.findById(req.positionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position invalid"));

        var languages = new HashSet<>(languageRepo.findAllById(req.languageIds));
        if (languages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limbaje invalide");
        }

        // 1) Salvează request-ul
        InterviewRequest ir = new InterviewRequest();
        ir.setUser(currentUser);
        ir.setLevel(level);
        ir.setPosition(position);
        ir.setLanguages(languages);
        ir = requestRepo.save(ir);

        // 2) Creează interviul
        Interview interview = new Interview();
        interview.setUser(currentUser);
        interview.setRequest(ir);
        interview.setStatus("GENERATED");
        interview = interviewRepo.save(interview);

        // 3) Selectează întrebări strict pe filtre, cu număr fix
        final int HR_COUNT = 2;
        final int TECH_COUNT = 4;
        final int PROBLEM_COUNT = 2;

        Long hrId = categoryRepo.findByName("HR")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Category HR missing")).getId();
        Long techId = categoryRepo.findByName("TECH")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Category TECH missing")).getId();
        Long problemId = categoryRepo.findByName("PROBLEM")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Category PROBLEM missing")).getId();

        var hrQs = pickRandom(
                questionRepo.findMatching(hrId, level.getId(), position.getId(), req.languageIds),
                HR_COUNT, "HR");

        var techQs = pickRandom(
                questionRepo.findMatching(techId, level.getId(), position.getId(), req.languageIds),
                TECH_COUNT, "TECH");

        var problemQs = pickRandom(
                questionRepo.findMatching(problemId, level.getId(), position.getId(), req.languageIds),
                PROBLEM_COUNT, "PROBLEM");

        // combină + shuffle pentru ordine variată
        var all = new java.util.ArrayList<com.licenta.backend.questions.entity.Question>();
        all.addAll(hrQs);
        all.addAll(techQs);
        all.addAll(problemQs);
        java.util.Collections.shuffle(all);

        // 4) Salvează în interview_questions
        int idx = 1;
        for (var q : all) {
            InterviewQuestion iq = new InterviewQuestion();
            iq.setInterview(interview);
            iq.setQuestion(q);
            iq.setOrderIndex(idx++);
            interviewQuestionRepo.save(iq);
        }

        return interview;
    }

    private List<Question> pickRandom(
            List<com.licenta.backend.questions.entity.Question> pool,
            int count,
            String categoryName) {

        if (pool.size() < count) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nu sunt suficiente întrebări pentru " + categoryName + ". Necesare: " + count + ", disponibile: " + pool.size()
            );
        }
        java.util.Collections.shuffle(pool);
        return pool.subList(0, count);
    }
    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String email = auth.getName().trim().toLowerCase();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public InterviewDetailsResponseDto getInterview(Long id) {

        Interview interview = interviewRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        var interviewQuestions = interviewQuestionRepo.fetchDetails(id);

        InterviewDetailsResponseDto dto = new InterviewDetailsResponseDto();
        dto.id = interview.getId();
        dto.status = interview.getStatus();

        dto.questions = interviewQuestions.stream().map(iq -> {

            var q = iq.getQuestion();

            InterviewDetailsResponseDto.QuestionItem item =
                    new InterviewDetailsResponseDto.QuestionItem();

            item.interviewQuestionId = iq.getId();
            item.questionId = q.getId();
            item.text = q.getText();
            item.category = q.getCategory().getName();
            item.answerType = q.getAnswerType();
            item.starterCode = getStarterCodeForQuestion(q);

            item.languages = q.getLanguages()
                    .stream()
                    .map(l -> l.getName())
                    .toList();

            // doar pentru MCQ
            if ("MCQ".equals(q.getAnswerType())) {
                item.options = q.getOptions().stream().map(opt -> {
                    InterviewDetailsResponseDto.OptionItem o =
                            new InterviewDetailsResponseDto.OptionItem();
                    o.id = opt.getId();
                    o.text = opt.getText();
                    return o;
                }).toList();
            } else {
                item.options = List.of();
            }

            return item;

        }).toList();

        return dto;
    }

    private String getStarterCodeForQuestion(Question question) {
        if (!"CODE".equals(question.getAnswerType())) {
            return null;
        }

        if (question.getStarterCode() != null && !question.getStarterCode().isBlank()) {
            return question.getStarterCode();
        }

        String preferredLanguage = question.getLanguages().stream()
                .map(language -> language.getName().toLowerCase())
                .findFirst()
                .orElse("java");
        String text = question.getText() == null ? "" : question.getText().toLowerCase();

        if (preferredLanguage.contains("python")) {
            return getPythonStarterCode(text);
        }

        if ("c".equals(preferredLanguage)) {
            return getCStarterCode(text);
        }

        if (preferredLanguage.contains("c++") || preferredLanguage.contains("cpp")) {
            return getCppStarterCode(text);
        }

        return getJavaStarterCode(text);
    }

    private String getJavaStarterCode(String text) {
        if (text.contains("palindrom")) {
            return """
                    import java.util.*;

                    public class Main {
                        public static boolean isPalindrome(String text) {
                            // Completeaza doar corpul acestei functii.
                            return false;
                        }

                        public static void main(String[] args) {
                            Scanner scanner = new Scanner(System.in);
                            String text = scanner.hasNextLine() ? scanner.nextLine() : "";
                            System.out.print(isPalindrome(text));
                        }
                    }
                    """;
        }

        if (text.contains("perech")) {
            return """
                    import java.util.*;

                    public class Main {
                        public static String findPairs(int[] numbers, int target) {
                            // Completeaza doar corpul acestei functii.
                            // Returneaza perechile ca text, de exemplu: "1 4, 2 3".
                            return "";
                        }

                        public static void main(String[] args) {
                            Scanner scanner = new Scanner(System.in);
                            int n = scanner.hasNextInt() ? scanner.nextInt() : 0;
                            int[] numbers = new int[n];
                            for (int i = 0; i < n; i++) {
                                numbers[i] = scanner.nextInt();
                            }
                            int target = scanner.hasNextInt() ? scanner.nextInt() : 0;
                            System.out.print(findPairs(numbers, target));
                        }
                    }
                    """;
        }

        return """
                import java.util.*;

                public class Main {
                    public static String solve(String input) {
                        // Completeaza doar corpul acestei functii.
                        return "";
                    }

                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        StringBuilder input = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            if (input.length() > 0) {
                                input.append("\\n");
                            }
                            input.append(scanner.nextLine());
                        }
                        System.out.print(solve(input.toString()));
                    }
                }
                """;
    }

    private String getPythonStarterCode(String text) {
        if (text.contains("palindrom")) {
            return """
                    def is_palindrome(text):
                        # Completeaza doar corpul acestei functii.
                        return False


                    if __name__ == "__main__":
                        text = input().strip()
                        print(str(is_palindrome(text)).lower())
                    """;
        }

        if (text.contains("perech")) {
            return """
                    def find_pairs(numbers, target):
                        # Completeaza doar corpul acestei functii.
                        # Returneaza perechile ca text, de exemplu: "1 4, 2 3".
                        return ""


                    if __name__ == "__main__":
                        n = int(input().strip())
                        numbers = list(map(int, input().split())) if n > 0 else []
                        target = int(input().strip())
                        print(find_pairs(numbers, target), end="")
                    """;
        }

        return """
                def solve(input_data):
                    # Completeaza doar corpul acestei functii.
                    return ""


                if __name__ == "__main__":
                    import sys
                    print(solve(sys.stdin.read()), end="")
                """;
    }

    private String getCppStarterCode(String text) {
        if (text.contains("palindrom")) {
            return """
                    #include <bits/stdc++.h>
                    using namespace std;

                    bool isPalindrome(string text) {
                        // Completeaza doar corpul acestei functii.
                        return false;
                    }

                    int main() {
                        string text;
                        getline(cin, text);
                        cout << (isPalindrome(text) ? "true" : "false");
                        return 0;
                    }
                    """;
        }

        if (text.contains("perech")) {
            return """
                    #include <bits/stdc++.h>
                    using namespace std;

                    string findPairs(vector<int> numbers, int target) {
                        // Completeaza doar corpul acestei functii.
                        // Returneaza perechile ca text, de exemplu: "1 4, 2 3".
                        return "";
                    }

                    int main() {
                        int n;
                        cin >> n;
                        vector<int> numbers(n);
                        for (int i = 0; i < n; i++) {
                            cin >> numbers[i];
                        }
                        int target;
                        cin >> target;
                        cout << findPairs(numbers, target);
                        return 0;
                    }
                    """;
        }

        return """
                #include <bits/stdc++.h>
                using namespace std;

                string solve(string input) {
                    // Completeaza doar corpul acestei functii.
                    return "";
                }

                int main() {
                    string input((istreambuf_iterator<char>(cin)), istreambuf_iterator<char>());
                    cout << solve(input);
                    return 0;
                }
                """;
    }

    private String getCStarterCode(String text) {
        if (text.contains("palindrom")) {
            return """
                    #include <stdio.h>
                    #include <string.h>
                    #include <stdbool.h>

                    bool is_palindrome(char text[]) {
                        // Completeaza doar corpul acestei functii.
                        return false;
                    }

                    int main() {
                        char text[1000];
                        if (fgets(text, sizeof(text), stdin) == NULL) {
                            return 0;
                        }
                        text[strcspn(text, "\\n")] = 0;
                        printf("%s", is_palindrome(text) ? "true" : "false");
                        return 0;
                    }
                    """;
        }

        if (text.contains("perech")) {
            return """
                    #include <stdio.h>

                    void print_pairs(int numbers[], int n, int target) {
                        // Completeaza doar corpul acestei functii.
                        // Afiseaza perechile ca text, de exemplu: "1 4, 2 3".
                    }

                    int main() {
                        int n;
                        scanf("%d", &n);
                        int numbers[1000];
                        for (int i = 0; i < n; i++) {
                            scanf("%d", &numbers[i]);
                        }
                        int target;
                        scanf("%d", &target);
                        print_pairs(numbers, n, target);
                        return 0;
                    }
                    """;
        }

        return """
                #include <stdio.h>

                void solve(void) {
                    // Completeaza doar corpul acestei functii.
                }

                int main() {
                    solve();
                    return 0;
                }
                """;
    }

    @Transactional
    public void submitAnswers(Long interviewId, SubmitInterviewAnswersRequestDto req) {
        if (req == null || req.answers == null || req.answers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu ai trimis răspunsuri.");
        }

        Interview interview = interviewRepo.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        User currentUser = getCurrentUser();
        if (!interview.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest interviu.");
        }

        for (var answerItem : req.answers) {
            if (answerItem.interviewQuestionId == null) continue;

            InterviewQuestion interviewQuestion = interviewQuestionRepo.findById(answerItem.interviewQuestionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview question invalid"));

            if (!interviewQuestion.getInterview().getId().equals(interviewId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Întrebarea nu aparține interviului.");
            }

            InterviewAnswer answer = interviewAnswerRepo
                    .findByInterviewQuestionId(interviewQuestion.getId())
                    .orElseGet(InterviewAnswer::new);

            answer.setInterviewQuestion(interviewQuestion);

            String answerType = interviewQuestion.getQuestion().getAnswerType();

            if ("MCQ".equals(answerType)) {
                if (answerItem.selectedOptionId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lipsește opțiunea selectată pentru o întrebare grilă.");
                }

                var selectedOption = questionOptionRepo.findById(answerItem.selectedOptionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opțiune invalidă"));

                answer.setSelectedOption(selectedOption);
                answer.setAnswerText(null);
            } else {
                answer.setAnswerText(answerItem.answerText);
                answer.setSelectedOption(null);
            }

            interviewAnswerRepo.save(answer);
        }
    }

    @Transactional
    public InterviewResultDto finishInterview(Long interviewId) {
        Interview interview = interviewRepo.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        User currentUser = getCurrentUser();
        if (!interview.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest interviu.");
        }

        var answers = interviewAnswerRepo.findByInterviewQuestionInterviewId(interviewId);

        int totalMcq = 0;
        int correctMcq = 0;

        for (InterviewAnswer answer : answers) {
            if ("MCQ".equals(answer.getInterviewQuestion().getQuestion().getAnswerType())) {
                totalMcq++;
                if (answer.getSelectedOption() != null && answer.getSelectedOption().isCorrect()) {
                    correctMcq++;
                }
            }
        }

        // Salvăm status COMPLETED înainte de analiza AI
        interview.setStatus("COMPLETED");
        interviewRepo.save(interview);

        // Generăm feedback AI pentru răspunsurile text/code
        analyzeInterviewAnswers(interviewId);

        // Calculăm scorul combinat: fiecare întrebare are greutate egală
        // MCQ: 100 dacă corect, 0 dacă greșit
        // TEXT/CODE: scor AI * 10 (0–10 → 0–100)
        var allFeedbacks = answerFeedbackRepo.findByInterviewAnswerInterviewQuestionInterviewId(interviewId);

        java.util.List<Integer> questionScores = new java.util.ArrayList<>();
        for (InterviewAnswer answer : answers) {
            String answerType = answer.getInterviewQuestion().getQuestion().getAnswerType();
            if ("MCQ".equals(answerType)) {
                boolean correct = answer.getSelectedOption() != null && answer.getSelectedOption().isCorrect();
                questionScores.add(correct ? 100 : 0);
            }
        }
        for (AnswerFeedback fb : allFeedbacks) {
            if (fb.getScore() != null) {
                questionScores.add(fb.getScore() * 10);
            }
        }

        int finalScore = questionScores.isEmpty() ? 0
                : (int) Math.round(questionScores.stream().mapToInt(Integer::intValue).average().orElse(0));

        interview.setScore(finalScore);
        interviewRepo.save(interview);

        generateInterviewOverview(interviewId);

        InterviewFeedbackSummary summary = interviewFeedbackSummaryRepo
                .findByInterviewId(interviewId)
                .orElse(null);

        InterviewResultDto dto = new InterviewResultDto();
        dto.interviewId = interview.getId();
        dto.status = interview.getStatus();
        dto.totalMcq = totalMcq;
        dto.correctMcq = correctMcq;
        dto.score = finalScore;

        if (summary != null) {
            dto.readyLevel = summary.getReadyLevel();
            dto.summaryText = summary.getSummaryText();
            dto.strongPoints = summary.getStrongPoints();
            dto.weakPoints = summary.getWeakPoints();
            dto.recommendedTopics = summary.getRecommendedTopics();
            dto.recommendedProblemCategories = summary.getRecommendedProblemCategories();
            dto.nextSteps = summary.getNextSteps();
        }

        return dto;
    }

    @Transactional
    public List<InterviewHistoryItemDto> getHistory() {
        User currentUser = getCurrentUser();

        var interviews = interviewRepo.findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        return interviews.stream().map(interview -> {
            InterviewHistoryItemDto item = new InterviewHistoryItemDto();
            item.interviewId = interview.getId();
            item.status = interview.getStatus();
            item.score = interview.getScore();
            item.createdAt = interview.getCreatedAt();
            item.level = interview.getRequest().getLevel().getName();
            item.position = interview.getRequest().getPosition().getName();

            var summary = interviewFeedbackSummaryRepo.findByInterviewId(interview.getId()).orElse(null);
            item.readyLevel = summary != null ? summary.getReadyLevel() : null;

            return item;
        }).toList();
    }

    @Transactional
    public void analyzeInterviewAnswers(Long interviewId) {
        Interview interview = interviewRepo.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        User currentUser = getCurrentUser();
        if (!interview.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest interviu.");
        }

        var answers = interviewAnswerRepo.findByInterviewQuestionInterviewId(interviewId);

        for (InterviewAnswer answer : answers) {
            String answerType = answer.getInterviewQuestion().getQuestion().getAnswerType();

            if (!"TEXT".equals(answerType) && !"CODE".equals(answerType)) {
                continue;
            }

            var aiResult = aiFeedbackService.analyzeAnswer(answer);

            AnswerFeedback feedback = answerFeedbackRepo
                    .findByInterviewAnswerId(answer.getId())
                    .orElseGet(AnswerFeedback::new);

            if (isFallbackFeedback(aiResult.feedbackJson) && hasRealFeedback(feedback)) {
                continue;
            }

            feedback.setInterviewAnswer(answer);
            feedback.setScore(aiResult.score);
            feedback.setGood(aiResult.good);
            feedback.setStrengths(aiResult.strengths);
            feedback.setWeaknesses(aiResult.weaknesses);
            feedback.setImprovementTips(aiResult.improvementTips);
            feedback.setSuggestedAnswer(aiResult.suggestedAnswer);
            feedback.setFeedbackJson(aiResult.feedbackJson);

            answerFeedbackRepo.save(feedback);
        }
    }

    private boolean isFallbackFeedback(String feedbackJson) {
        return feedbackJson != null && feedbackJson.contains("\"fallback\":true");
    }

    private boolean hasRealFeedback(AnswerFeedback feedback) {
        String feedbackJson = feedback.getFeedbackJson();
        return feedback.getId() != null
                && feedbackJson != null
                && !feedbackJson.contains("\"fallback\":true");
    }

    public InterviewReviewResponseDto getInterviewReview(Long interviewId) {
        Interview interview = interviewRepo.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        User currentUser = getCurrentUser();
        if (!interview.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest interviu.");
        }

        var interviewQuestions = interviewQuestionRepo.fetchDetails(interviewId);
        var answers = interviewAnswerRepo.findByInterviewQuestionInterviewId(interviewId);

        java.util.Map<Long, InterviewAnswer> answersByInterviewQuestionId = answers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getInterviewQuestion().getId(),
                        a -> a
                ));

        InterviewReviewResponseDto dto = new InterviewReviewResponseDto();
        dto.interviewId = interview.getId();
        dto.status = interview.getStatus();
        dto.score = interview.getScore();

        dto.questions = interviewQuestions.stream().map(interviewQuestion -> {
            var question = interviewQuestion.getQuestion();
            var answer = answersByInterviewQuestionId.get(interviewQuestion.getId());

            InterviewReviewResponseDto.QuestionReviewItem item =
                    new InterviewReviewResponseDto.QuestionReviewItem();

            item.interviewQuestionId = interviewQuestion.getId();
            item.text = question.getText();
            item.category = question.getCategory().getName();
            item.answerType = question.getAnswerType();

            item.languages = question.getLanguages()
                    .stream()
                    .map(language -> language.getName())
                    .toList();

            if ("MCQ".equals(question.getAnswerType())) {
                item.options = question.getOptions().stream().map(option -> {
                    InterviewReviewResponseDto.OptionItem optionItem =
                            new InterviewReviewResponseDto.OptionItem();
                    optionItem.id = option.getId();
                    optionItem.text = option.getText();
                    optionItem.correct = option.isCorrect();
                    return optionItem;
                }).toList();
            } else {
                item.options = List.of();
            }

            item.answerText = answer != null ? answer.getAnswerText() : null;
            item.selectedOptionId =
                    (answer != null && answer.getSelectedOption() != null)
                            ? answer.getSelectedOption().getId()
                            : null;

            if (answer != null) {
                var feedbackOpt = answerFeedbackRepo
                        .findByInterviewAnswerId(answer.getId());

                feedbackOpt.ifPresent(fb -> {
                    item.aiScore = fb.getScore();
                    item.aiGood = fb.getGood();
                    item.aiStrengths = fb.getStrengths();
                    item.aiWeaknesses = fb.getWeaknesses();
                    item.aiImprovementTips = fb.getImprovementTips();
                    item.aiSuggestedAnswer = fb.getSuggestedAnswer();
                    item.aiFeedbackJson = fb.getFeedbackJson();
                });
            }

            return item;
        }).toList();

        interviewFeedbackSummaryRepo.findByInterviewId(interviewId).ifPresent(summary -> {
            InterviewReviewResponseDto.FeedbackSummaryDto fs = new InterviewReviewResponseDto.FeedbackSummaryDto();
            fs.readyLevel = summary.getReadyLevel();
            fs.summaryText = summary.getSummaryText();
            fs.strongPoints = summary.getStrongPoints();
            fs.weakPoints = summary.getWeakPoints();
            fs.recommendedTopics = summary.getRecommendedTopics();
            fs.recommendedProblemCategories = summary.getRecommendedProblemCategories();
            fs.nextSteps = summary.getNextSteps();
            dto.feedbackSummary = fs;
        });

        return dto;
    }

    @Transactional
    public void generateInterviewOverview(Long interviewId) {
        Interview interview = interviewRepo.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        User currentUser = getCurrentUser();
        if (!interview.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest interviu.");
        }

        var answerFeedbackList = answerFeedbackRepo.findByInterviewAnswerInterviewQuestionInterviewId(interviewId);

        int mcqScore = interview.getScore() == null ? 0 : interview.getScore();

        InterviewOverviewResultDto overviewResult =
                aiFeedbackService.analyzeInterviewOverview(mcqScore, answerFeedbackList);

        InterviewFeedbackSummary summary = interviewFeedbackSummaryRepo
                .findByInterviewId(interviewId)
                .orElseGet(InterviewFeedbackSummary::new);

        summary.setInterview(interview);
        summary.setReadyLevel(overviewResult.readyLevel);
        summary.setSummaryText(overviewResult.summaryText);
        summary.setStrongPoints(overviewResult.strongPoints);
        summary.setWeakPoints(overviewResult.weakPoints);
        summary.setRecommendedTopics(overviewResult.recommendedTopics);
        summary.setRecommendedProblemCategories(overviewResult.recommendedProblemCategories);
        summary.setNextSteps(overviewResult.nextSteps);

        interviewFeedbackSummaryRepo.save(summary);
    }

    @Transactional
    public InterviewResultDto getInterviewResult(Long interviewId) {
        Interview interview = interviewRepo.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        User currentUser = getCurrentUser();
        if (!interview.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest interviu.");
        }

        var answers = interviewAnswerRepo.findByInterviewQuestionInterviewId(interviewId);

        int totalMcq = 0;
        int correctMcq = 0;

        for (InterviewAnswer answer : answers) {
            String answerType = answer.getInterviewQuestion().getQuestion().getAnswerType();

            if ("MCQ".equals(answerType)) {
                totalMcq++;

                if (answer.getSelectedOption() != null && answer.getSelectedOption().isCorrect()) {
                    correctMcq++;
                }
            }
        }

        InterviewFeedbackSummary summary = interviewFeedbackSummaryRepo
                .findByInterviewId(interviewId)
                .orElse(null);

        InterviewResultDto dto = new InterviewResultDto();
        dto.interviewId = interview.getId();
        dto.status = interview.getStatus();
        dto.totalMcq = totalMcq;
        dto.correctMcq = correctMcq;
        dto.score = interview.getScore() == null ? 0 : interview.getScore();

        if (summary != null) {
            dto.readyLevel = summary.getReadyLevel();
            dto.summaryText = summary.getSummaryText();
            dto.strongPoints = summary.getStrongPoints();
            dto.weakPoints = summary.getWeakPoints();
            dto.recommendedTopics = summary.getRecommendedTopics();
            dto.recommendedProblemCategories = summary.getRecommendedProblemCategories();
            dto.nextSteps = summary.getNextSteps();
        }

        return dto;
    }

    @Transactional
    public java.util.List<com.licenta.backend.interviews.api.dto.RecommendationItemDto> getRecommendations() {
        User currentUser = getCurrentUser();
        var summaries = interviewFeedbackSummaryRepo
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        return summaries.stream().map(summary -> {
            var dto = new com.licenta.backend.interviews.api.dto.RecommendationItemDto();
            var interview = summary.getInterview();
            dto.interviewId = interview.getId();
            dto.position = interview.getRequest().getPosition().getName();
            dto.level = interview.getRequest().getLevel().getName();
            dto.createdAt = interview.getCreatedAt().toString();
            dto.score = interview.getScore();
            dto.readyLevel = summary.getReadyLevel();
            dto.recommendedTopics = summary.getRecommendedTopics();
            dto.recommendedProblemCategories = summary.getRecommendedProblemCategories();
            dto.nextSteps = summary.getNextSteps();
            return dto;
        }).toList();
    }

    @Transactional
    public void deleteInterview(Long interviewId) {
        Interview interview = interviewRepo.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        User currentUser = getCurrentUser();
        if (!interview.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest interviu.");
        }

        Long requestId = interview.getRequest().getId();

        // Delete in correct order to respect FK constraints
        interviewFeedbackSummaryRepo.findByInterviewId(interviewId)
                .ifPresent(interviewFeedbackSummaryRepo::delete);

        var feedbacks = answerFeedbackRepo.findByInterviewAnswerInterviewQuestionInterviewId(interviewId);
        answerFeedbackRepo.deleteAll(feedbacks);

        var answers = interviewAnswerRepo.findByInterviewQuestionInterviewId(interviewId);
        interviewAnswerRepo.deleteAll(answers);

        var questions = interviewQuestionRepo.findByInterviewIdOrderByOrderIndexAsc(interviewId);
        interviewQuestionRepo.deleteAll(questions);

        interviewRepo.delete(interview);
        requestRepo.deleteById(requestId);
    }
}
