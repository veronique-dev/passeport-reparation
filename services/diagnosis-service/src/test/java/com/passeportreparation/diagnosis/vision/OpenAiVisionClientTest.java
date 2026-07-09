package com.passeportreparation.diagnosis.vision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiVisionClientTest {

    private OpenAiVisionClient client;

    @BeforeEach
    void setUp() {
        client = new OpenAiVisionClient(new ObjectMapper());
    }

    @Test
    void parsesSupportedOvenSuggestion() throws Exception {
        String json = """
                {
                  "category": "OVEN",
                  "applianceLabel": "Four encastrable",
                  "suggestedIssueCode": "OV_DOOR_SEAL",
                  "probableIssue": "Joint de porte usé",
                  "confidence": 0.81,
                  "supported": true,
                  "rationale": "Porte de four visible"
                }
                """;

        VisionSuggestion suggestion = client.parseSuggestion(json);

        assertThat(suggestion.category()).isEqualTo(ApplianceCategory.OVEN);
        assertThat(suggestion.suggestedIssueCode()).isEqualTo(IssueCode.OV_DOOR_SEAL);
        assertThat(suggestion.supported()).isTrue();
        assertThat(suggestion.confidence()).isEqualTo(0.81);
    }

    @Test
    void mapsUnknownCategoryToUnsupported() throws Exception {
        String json = """
                {
                  "category": "IRON",
                  "applianceLabel": "Fer à repasser",
                  "suggestedIssueCode": "WM_DRAIN_PUMP",
                  "probableIssue": "Ne chauffe plus",
                  "confidence": 0.9,
                  "supported": true,
                  "rationale": "Fer détecté"
                }
                """;

        VisionSuggestion suggestion = client.parseSuggestion(json);

        assertThat(suggestion.category()).isEqualTo(ApplianceCategory.UNSUPPORTED);
        assertThat(suggestion.supported()).isFalse();
    }
}
