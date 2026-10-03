package com.claimwise.dto;

import com.claimwise.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimStatusUpdateRequest {

    @NotNull(message = "Target status is required")
    private ClaimStatus status;

    private String comment;
}
