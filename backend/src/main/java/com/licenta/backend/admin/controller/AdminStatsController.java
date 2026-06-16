package com.licenta.backend.admin.controller;

import com.licenta.backend.admin.dto.AdminStatsDto;
import com.licenta.backend.admin.service.AdminStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/stats")
    public AdminStatsDto getStats(@RequestParam(defaultValue = "1m") String period) {
        return adminStatsService.getStats(period);
    }
}
