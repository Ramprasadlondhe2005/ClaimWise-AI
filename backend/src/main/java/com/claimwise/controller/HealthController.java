package com.claimwise.controller;

import com.claimwise.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        Map<String, String> healthStatus = Map.of(
                "status", "UP",
                "service", "Expense Claim Policy Review Assistant",
                "version", "1.0.0"
        );
        return ResponseEntity.ok(ApiResponse.success(healthStatus, "System is operating normally"));
    }
}
