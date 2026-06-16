package com.licenta.backend.admin.controller;

import com.licenta.backend.interviews.core.entity.Interview;
import com.licenta.backend.interviews.core.repo.InterviewRepo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/export")
public class AdminExportController {

    private final InterviewRepo interviewRepo;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Europe/Bucharest"));

    public AdminExportController(InterviewRepo interviewRepo) {
        this.interviewRepo = interviewRepo;
    }

    @GetMapping("/interviews")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportInterviews() {
        List<Interview> all = interviewRepo.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Email,Prenume,Nume,Poziție,Nivel,Status,Scor,Data creare\n");

        for (Interview iv : all) {
            String email     = iv.getUser() != null ? iv.getUser().getEmail() : "";
            String firstName = iv.getUser() != null ? nullSafe(iv.getUser().getFirstName()) : "";
            String lastName  = iv.getUser() != null ? nullSafe(iv.getUser().getLastName())  : "";
            String position  = iv.getRequest() != null && iv.getRequest().getPosition() != null
                    ? iv.getRequest().getPosition().getName() : "";
            String level     = iv.getRequest() != null && iv.getRequest().getLevel() != null
                    ? iv.getRequest().getLevel().getName() : "";
            String status    = nullSafe(iv.getStatus());
            String score     = iv.getScore() != null ? iv.getScore().toString() : "";
            String date      = iv.getCreatedAt() != null ? FMT.format(iv.getCreatedAt()) : "";

            csv.append(iv.getId()).append(",")
               .append(quote(email)).append(",")
               .append(quote(firstName)).append(",")
               .append(quote(lastName)).append(",")
               .append(quote(position)).append(",")
               .append(quote(level)).append(",")
               .append(quote(status)).append(",")
               .append(score).append(",")
               .append(quote(date)).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"interviuri.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    private String nullSafe(String s) { return s == null ? "" : s; }
    private String quote(String s)    { return "\"" + s.replace("\"", "\"\"") + "\""; }
}
