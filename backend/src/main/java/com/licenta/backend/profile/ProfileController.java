package com.licenta.backend.profile;

import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import com.licenta.backend.profile.dto.UpdateUserProfileRequest;
import com.licenta.backend.profile.dto.UserProfileDto;
import com.licenta.backend.users.entity.User;
import com.licenta.backend.users.repo.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepo userRepo;
    private final LevelRepository levelRepo;
    private final PositionRepository positionRepo;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepo userRepo, LevelRepository levelRepo,
                             PositionRepository positionRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.levelRepo = levelRepo;
        this.positionRepo = positionRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UserProfileDto me(Authentication authentication) {
        return toDto(currentUser(authentication));
    }

    @PutMapping("/me")
    @Transactional
    public UserProfileDto updateMe(Authentication authentication, @RequestBody UpdateUserProfileRequest req) {
        User user = currentUser(authentication);

        String firstName = req.firstName == null ? "" : req.firstName.trim();
        String lastName = req.lastName == null ? "" : req.lastName.trim();
        if (firstName.length() < 2 || lastName.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completeaza numele si prenumele.");
        }
        if (req.levelId == null || req.positionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecteaza nivelul si pozitia.");
        }

        var level = levelRepo.findById(req.levelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel invalid."));
        var position = positionRepo.findById(req.positionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pozitie invalida."));

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProfileLevel(level);
        user.setProfilePosition(position);

        return toDto(userRepo.save(user));
    }

    @PutMapping("/password")
    @Transactional
    public Map<String, String> changePassword(Authentication authentication,
                                              @RequestBody Map<String, String> body) {
        String currentPassword = body.getOrDefault("currentPassword", "");
        String newPassword     = body.getOrDefault("newPassword", "");
        String confirm         = body.getOrDefault("confirmPassword", "");

        if (currentPassword.isBlank() || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completează toate câmpurile.");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parola nouă trebuie să aibă minim 6 caractere.");
        }
        if (!newPassword.equals(confirm)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parolele nu coincid.");
        }

        User user = currentUser(authentication);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parola curentă este incorectă.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        return Map.of("message", "Parola a fost schimbată cu succes.");
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Neautentificat.");
        }
        return userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator inexistent."));
    }

    private UserProfileDto toDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.userId = user.getId();
        dto.email = user.getEmail();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.roles = user.getRoles().stream().map(role -> role.getName()).toList();

        if (user.getProfileLevel() != null) {
            dto.levelId = user.getProfileLevel().getId();
            dto.levelName = user.getProfileLevel().getName();
        }
        if (user.getProfilePosition() != null) {
            dto.positionId = user.getProfilePosition().getId();
            dto.positionName = user.getProfilePosition().getName();
        }

        return dto;
    }
}
