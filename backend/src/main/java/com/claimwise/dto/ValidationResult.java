package com.claimwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean passed;
    private String status; // PASS, WARNING, FAILED
    @Builder.Default
    private List<ValidationIssue> issues = new ArrayList<>();

    public static ValidationResult pass() {
        return ValidationResult.builder()
                .passed(true)
                .status("PASS")
                .issues(new ArrayList<>())
                .build();
    }
}
