package com.claimwise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiOverrideRequest {

    @NotBlank(message = "New target classification is required")
    private String newClassification; // COMPLIANT, NEEDS_CLARIFICATION, NEEDS_REVIEW, NON_COMPLIANT

    @NotBlank(message = "Mandatory override reason is required")
    private String reason;
}
