package com.licenta.backend.config;

import com.licenta.backend.users.entity.Role;
import com.licenta.backend.users.entity.User;
import com.licenta.backend.users.repo.RoleRepo;
import com.licenta.backend.users.repo.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepo userRepo, RoleRepo roleRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        System.out.println("DATASEEDER RUNNING...");

        Role userRole = roleRepo.findByName("USER")
                .orElseGet(() -> roleRepo.save(new Role("USER")));

        boolean exists = userRepo.existsByEmail("test@test.com");
        System.out.println("Exists test@test.com? " + exists);

        if (!exists) {
            User u = new User();
            u.setEmail("test@test.com");
            u.setPassword(encoder.encode("123456"));
            u.setRoles(Set.of(userRole));
            userRepo.save(u);
            System.out.println("Seeded user test@test.com / 123456");
        }
    }
}