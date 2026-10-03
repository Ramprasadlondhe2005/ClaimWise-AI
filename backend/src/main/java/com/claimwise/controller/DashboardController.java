package com.claimwise.controller;

import com.claimwise.dto.ApiResponse;
import com.claimwise.dto.DashboardMetricsResponse;
import com.claimwise.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardStats() {
        DashboardMetricsResponse metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics, "Live dashboard metrics retrieved"));
    }
}
