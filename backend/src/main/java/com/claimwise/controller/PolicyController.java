package com.claimwise.controller;

import com.claimwise.dto.ApiResponse;
import com.claimwise.dto.PolicyResponse;
import com.claimwise.entity.ExpenseCategory;
import com.claimwise.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getAllActivePolicies() {
        List<PolicyResponse> policies = policyService.getAllActivePolicies();
        return ResponseEntity.ok(ApiResponse.success(policies, "Active organizational policies retrieved"));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getPoliciesByCategory(@PathVariable ExpenseCategory category) {
        List<PolicyResponse> policies = policyService.getPoliciesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(policies, "Policies for category " + category + " retrieved"));
    }

    @GetMapping("/code/{policyCode}")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicyByCode(@PathVariable String policyCode) {
        PolicyResponse policy = policyService.getPolicyByCode(policyCode);
        return ResponseEntity.ok(ApiResponse.success(policy, "Policy rule details retrieved"));
    }
}
