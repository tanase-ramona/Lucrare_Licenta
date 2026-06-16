package com.licenta.backend.config;

import com.licenta.backend.users.entity.Role;
import com.licenta.backend.users.repo.RoleRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RolesInitializer implements CommandLineRunner {

    private final RoleRepo roleRepo;

    public RolesInitializer(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Override
    public void run(String... args) {
        createIfMissing("USER");
        createIfMissing("ADMIN");
    }

    private void createIfMissing(String name) {
        roleRepo.findByName(name).orElseGet(() -> roleRepo.save(new Role(name)));
    }
}