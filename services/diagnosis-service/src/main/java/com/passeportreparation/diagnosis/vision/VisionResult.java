package com.passeportreparation.diagnosis.vision;

import com.passeportreparation.common.enums.ApplianceCategory;

public record VisionResult(
        ApplianceCategory category,
        String applianceLabel,
        String probableIssue,
        double confidence,
        boolean supported
) {
}
