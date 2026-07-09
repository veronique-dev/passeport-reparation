package com.passeportreparation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostEstimateDto {
    private BigDecimal repairLow;
    private BigDecimal repairHigh;
    private BigDecimal replacementApprox;
    private String currency;
}
