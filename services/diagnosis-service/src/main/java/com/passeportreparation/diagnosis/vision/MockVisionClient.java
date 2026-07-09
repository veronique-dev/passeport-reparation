package com.passeportreparation.diagnosis.vision;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import com.passeportreparation.diagnosis.pricing.IssuePricing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Suggestion déterministe sans clé API — utile en local / CI.
 * Ne remplace jamais la confirmation utilisateur.
 */
@Component
@ConditionalOnProperty(name = "app.vision.provider", havingValue = "mock", matchIfMissing = true)
public class MockVisionClient implements VisionClient {

    private static final List<IssuePricing> SUPPORTED = IssuePricing.CATALOG.stream()
            .filter(p -> !p.code().name().endsWith("_UNKNOWN"))
            .toList();

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public VisionSuggestion suggest(String mediaId, byte[] imageBytes, String contentType) {
        if (mediaId != null && mediaId.toLowerCase().contains("unsupported")) {
            return unsupported("Mot-clé unsupported détecté dans mediaId (mock)");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return unsupported("Image vide — impossible de suggérer");
        }

        int index = Math.floorMod(mediaId == null ? 0 : mediaId.hashCode(), SUPPORTED.size());
        IssuePricing pick = SUPPORTED.get(index);
        return new VisionSuggestion(
                pick.category(),
                pick.applianceLabel(),
                pick.code(),
                pick.issueLabel(),
                0.62,
                true,
                "Suggestion mock basée sur un hash du mediaId — à confirmer / corriger."
        );
    }

    private static VisionSuggestion unsupported(String rationale) {
        return new VisionSuggestion(
                ApplianceCategory.UNSUPPORTED,
                "Appareil hors périmètre",
                IssueCode.UNSUPPORTED_OTHER,
                "Catégorie non supportée pour l’instant",
                0.35,
                false,
                rationale
        );
    }
}
