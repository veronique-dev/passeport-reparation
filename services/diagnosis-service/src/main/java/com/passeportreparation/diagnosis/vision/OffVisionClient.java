package com.passeportreparation.diagnosis.vision;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.vision.provider", havingValue = "off")
public class OffVisionClient implements VisionClient {

    @Override
    public String providerName() {
        return "off";
    }

    @Override
    public VisionSuggestion suggest(String mediaId, byte[] imageBytes, String contentType) {
        return new VisionSuggestion(
                ApplianceCategory.UNSUPPORTED,
                "Suggestion désactivée",
                IssueCode.UNSUPPORTED_OTHER,
                "VISION_PROVIDER=off — choisis manuellement la catégorie",
                0.0,
                false,
                "Provider vision désactivé"
        );
    }
}
