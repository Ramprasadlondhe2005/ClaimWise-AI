package com.claimwise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectClaimRequest {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
