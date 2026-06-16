package com.licenta.backend.auth.controller;

import com.licenta.backend.auth.dto.AuthResponse;
import com.licenta.backend.auth.dto.LoginRequest;
import com.licenta.backend.auth.security.JwtTokenProvider;
import com.licenta.backend.users.entity.User;
import com.licenta.backend.users.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.licenta.backend.auth.dto.RegisterRequest;
import com.licenta.backend.users.entity.Role;
import com.licenta.backend.users.repo.RoleRepo;
import com.licenta.backend.interviews.filters.repo.LevelRepository;
import com.licenta.backend.interviews.filters.repo.PositionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwt;
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final LevelRepository levelRepo;
    private final PositionRepository positionRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwt,
                          UserRepo userRepo,
                          RoleRepo roleRepo,
                          LevelRepository levelRepo,
                          PositionRepository positionRepo,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.levelRepo = levelRepo;
        this.positionRepo = positionRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.getPassword())
            );
        } catch (AuthenticationException ex) {
            log.warn("Login failed for email: {}", email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email sau parola gresita");
        }

        User u = userRepo.findByEmail(email).orElseThrow();
        String token = jwt.generateToken(email);

        var roles = u.getRoles().stream().map(r -> r.getName()).toList();
        return new AuthResponse(token, u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), roles);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {

        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        String password = req.getPassword() == null ? "" : req.getPassword();
        String confirmPassword = req.getConfirmPassword() == null ? "" : req.getConfirmPassword();
        String firstName = req.getFirstName() == null ? "" : req.getFirstName().trim();
        String lastName = req.getLastName() == null ? "" : req.getLastName().trim();

        if (email.isBlank() || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email invalid sau parola prea scurta (min 6).");
        }
        if (!password.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parolele nu coincid.");
        }
        if (firstName.length() < 2 || lastName.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completeaza numele si prenumele.");
        }
        if (req.getLevelId() == null || req.getPositionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecteaza nivelul si pozitia.");
        }

        if (userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exista deja un cont cu acest email.");
        }

        var level = levelRepo.findById(req.getLevelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel invalid."));
        var position = positionRepo.findById(req.getPositionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pozitie invalida."));

        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USER lipseste din DB"));

        User u = new User();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setPassword(passwordEncoder.encode(password));
        u.setProfileLevel(level);
        u.setProfilePosition(position);
        u.getRoles().add(userRole);

        userRepo.save(u);

        // Auto-login după register (îți întoarce token direct)
        String token = jwt.generateToken(email);
        var roles = u.getRoles().stream().map(r -> r.getName()).toList();
        return new AuthResponse(token, u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), roles);
    }


}
