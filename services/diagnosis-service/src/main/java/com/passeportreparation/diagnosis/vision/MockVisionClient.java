package com.passeportreparation.diagnosis.vision;

import com.passeportreparation.common.enums.ApplianceCategory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock vision client for local demo without an AI provider key.
 * Cycles through supported categories based on mediaId hash.
 */
@Component
@ConditionalOnProperty(name = "app.vision.provider", havingValue = "mock", matchIfMissing = true)
public class MockVisionClient implements VisionClient {

    private static final List<VisionResult> SAMPLES = List.of(
            new VisionResult(
                    ApplianceCategory.WASHING_MACHINE,
                    "Lave-linge",
                    "Ne vidange plus / erreur de pompe de vidange",
                    0.82,
                    true
            ),
            new VisionResult(
                    ApplianceCategory.DISHWASHER,
                    "Lave-vaisselle",
                    "Ne chauffe plus / résistance défaillante",
                    0.78,
                    true
            ),
            new VisionResult(
                    ApplianceCategory.OVEN,
                    "Four",
                    "Ne monte plus en température / thermostat",
                    0.75,
                    true
            )
    );

    @Override
    public VisionResult analyze(String mediaId) {
        if (mediaId != null && mediaId.toLowerCase().contains("unsupported")) {
            return new VisionResult(
                    ApplianceCategory.UNSUPPORTED,
                    "Appareil non reconnu",
                    "Catégorie hors périmètre MVP (lave-linge, lave-vaisselle, four)",
                    0.4,
                    false
            );
        }
        int index = Math.floorMod(mediaId == null ? 0 : mediaId.hashCode(), SAMPLES.size());
        VisionResult base = SAMPLES.get(index);
        double jitter = ThreadLocalRandom.current().nextDouble(-0.05, 0.05);
        return new VisionResult(
                base.category(),
                base.applianceLabel(),
                base.probableIssue(),
                Math.min(0.95, Math.max(0.5, base.confidence() + jitter)),
                true
        );
    }
}
