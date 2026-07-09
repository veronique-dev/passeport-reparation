package com.passeportreparation.diagnosis.vision;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;

public record VisionSuggestion(
        ApplianceCategory category,
        String applianceLabel,
        IssueCode suggestedIssueCode,
        String probableIssue,
        double confidence,
        boolean supported,
        String rationale
) {
}
