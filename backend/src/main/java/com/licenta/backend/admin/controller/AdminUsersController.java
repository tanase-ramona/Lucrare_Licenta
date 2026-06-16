package com.licenta.backend.admin.controller;

import com.licenta.backend.interviews.core.entity.Interview;
import com.licenta.backend.interviews.core.repo.InterviewRepo;
import com.licenta.backend.users.entity.Role;
import com.licenta.backend.users.entity.User;
import com.licenta.backend.users.repo.RoleRepo;
import com.licenta.backend.users.repo.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUsersController {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final InterviewRepo interviewRepo;

    public AdminUsersController(UserRepo userRepo, RoleRepo roleRepo, InterviewRepo interviewRepo) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.interviewRepo = interviewRepo;
    }

    public record UserAdminDto(
            Long id,
            String email,
            String firstName,
            String lastName,
            List<String> roles,
            long interviewCount,
            long completedCount,
            Double avgScore,
            String createdAt
    ) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserAdminDto> listUsers() {
        return userRepo.findAll().stream()
                .map(u -> {
                    List<Interview> iv = interviewRepo.findByUserIdOrderByCreatedAtDesc(u.getId());
                    long completed = iv.stream().filter(i -> "COMPLETED".equals(i.getStatus())).count();
                    OptionalDouble avg = iv.stream()
                            .filter(i -> i.getScore() != null)
                            .mapToInt(Interview::getScore)
                            .average();
                    Double avgRounded = avg.isPresent()
                            ? Math.round(avg.getAsDouble() * 10.0) / 10.0
                            : null;

                    return new UserAdminDto(
                            u.getId(),
                            u.getEmail(),
                            u.getFirstName() != null ? u.getFirstName() : "",
                            u.getLastName()  != null ? u.getLastName()  : "",
                            u.getRoles().stream().map(Role::getName).sorted().toList(),
                            iv.size(),
                            completed,
                            avgRounded,
                            u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
                    );
                })
                .sorted((a, b) -> {
                    boolean aAdmin = a.roles().contains("ADMIN");
                    boolean bAdmin = b.roles().contains("ADMIN");
                    if (aAdmin != bAdmin) return aAdmin ? -1 : 1;
                    return a.email().compareToIgnoreCase(b.email());
                })
                .toList();
    }

    @PutMapping("/{id}/role")
    @Transactional
    public Map<String, Object> toggleAdminRole(@PathVariable Long id,
                                               @RequestBody Map<String, Boolean> body) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (u.getEmail().equals(currentEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu îți poți modifica propriul rol.");
        }

        boolean makeAdmin = Boolean.TRUE.equals(body.get("admin"));
        Role adminRole = roleRepo.findByName("ADMIN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN role not found"));
        Role userRole  = roleRepo.findByName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USER role not found"));

        if (makeAdmin) {
            u.getRoles().add(adminRole);
        } else {
            u.getRoles().remove(adminRole);
            u.getRoles().add(userRole);
        }
        userRepo.save(u);

        return Map.of(
                "id", id,
                "admin", makeAdmin,
                "message", makeAdmin ? "Utilizatorul a primit rol Admin." : "Rolul Admin a fost retras."
        );
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> deleteUser(@PathVariable Long id) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (u.getEmail().equals(currentEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu îți poți șterge propriul cont.");
        }
        if (u.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu poți șterge un cont de Admin.");
        }

        userRepo.delete(u);
        return Map.of("message", "Contul a fost șters.");
    }
}
