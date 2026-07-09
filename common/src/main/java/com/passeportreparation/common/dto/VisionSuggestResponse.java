package com.passeportreparation.common.dto;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionSuggestResponse {
    private String mediaId;
    private ApplianceCategory category;
    private String applianceLabel;
    private IssueCode suggestedIssueCode;
    private String probableIssue;
    private double confidence;
    private boolean supported;
    private String provider;
    private String rationale;
    private boolean suggestionOnly;
}
