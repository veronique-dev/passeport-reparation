package com.passeportreparation.common.dto;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import com.passeportreparation.common.enums.RepairVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponse {
    private UUID id;
    private String mediaId;
    private ApplianceCategory category;
    private String applianceLabel;
    private IssueCode issueCode;
    private String probableIssue;
    private double confidence;
    private CostEstimateDto estimate;
    private RepairVerdict verdict;
    private String disclaimer;
    private boolean supported;
    private boolean userConfirmed;
    private UUID userId;
}
