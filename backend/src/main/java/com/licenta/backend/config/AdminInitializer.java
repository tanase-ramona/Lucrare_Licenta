package com.licenta.backend.config;

import com.licenta.backend.users.entity.Role;
import com.licenta.backend.users.entity.User;
import com.licenta.backend.users.repo.RoleRepo;
import com.licenta.backend.users.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(2)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepo userRepo, RoleRepo roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@admin.com";
        if (userRepo.existsByEmail(adminEmail)) return;

        Role adminRole = roleRepo.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found — run RolesInitializer first"));

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(Set.of(adminRole));
        userRepo.save(admin);

        log.info("Default admin created: {}", adminEmail);
    }
}
