package com.claimwise.controller;

import com.claimwise.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class RoleTestController {

    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'REVIEWER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> testEmployeeAccess() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Access granted to EMPLOYEE tier endpoint"),
                "Employee authorization verified"
        ));
    }

    @GetMapping("/reviewer")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> testReviewerAccess() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Access granted to REVIEWER tier endpoint"),
                "Reviewer authorization verified"
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> testAdminAccess() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Access granted to ADMIN tier endpoint"),
                "Admin authorization verified"
        ));
    }
}
