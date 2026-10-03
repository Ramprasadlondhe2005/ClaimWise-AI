package com.claimwise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClarificationClaimRequest {
    @NotBlank(message = "Clarification request message is required")
    private String message;
}
