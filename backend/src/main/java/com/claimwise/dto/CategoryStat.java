package com.claimwise.dto;

import com.claimwise.entity.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStat {
    private ExpenseCategory category;
    private long count;
    private BigDecimal totalAmount;
}
