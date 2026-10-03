package com.claimwise.dto;

import com.claimwise.entity.ExpenseCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateClaimRequest {

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive and greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g. USD, EUR, INR)")
    private String currency;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 2000, message = "Description must be between 5 and 2000 characters")
    private String description;

    private boolean receiptAvailable;
}
