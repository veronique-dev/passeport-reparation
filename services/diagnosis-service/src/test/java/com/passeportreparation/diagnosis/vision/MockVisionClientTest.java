package com.passeportreparation.diagnosis.vision;

import com.passeportreparation.common.enums.ApplianceCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockVisionClientTest {

    private final MockVisionClient client = new MockVisionClient();

    @Test
    void suggestsSupportedCategoryFromMediaId() {
        VisionSuggestion suggestion = client.suggest("abc-123", new byte[]{1, 2, 3}, "image/png");

        assertThat(suggestion.supported()).isTrue();
        assertThat(suggestion.category()).isIn(
                ApplianceCategory.WASHING_MACHINE,
                ApplianceCategory.DISHWASHER,
                ApplianceCategory.OVEN
        );
        assertThat(suggestion.suggestedIssueCode()).isNotNull();
        assertThat(client.providerName()).isEqualTo("mock");
    }

    @Test
    void returnsUnsupportedWhenMediaIdContainsKeyword() {
        VisionSuggestion suggestion = client.suggest("photo-unsupported-1", new byte[]{1}, "image/png");

        assertThat(suggestion.supported()).isFalse();
        assertThat(suggestion.category()).isEqualTo(ApplianceCategory.UNSUPPORTED);
    }
}
