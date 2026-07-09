package com.passeportreparation.diagnosis.service;

import com.passeportreparation.common.dto.VisionSuggestRequest;
import com.passeportreparation.common.dto.VisionSuggestResponse;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import com.passeportreparation.diagnosis.media.MediaClient;
import com.passeportreparation.diagnosis.vision.MockVisionClient;
import com.passeportreparation.diagnosis.vision.VisionClient;
import com.passeportreparation.diagnosis.vision.VisionSuggestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisionSuggestServiceTest {

    @Mock
    private MediaClient mediaClient;

    @Test
    void returnsSuggestionOnlyPayloadFromMock() {
        VisionSuggestService service = new VisionSuggestService(mediaClient, new MockVisionClient());
        when(mediaClient.fetch("m-1"))
                .thenReturn(new MediaClient.MediaImage("m-1", new byte[]{9, 9}, "image/png"));

        VisionSuggestResponse response = service.suggest(VisionSuggestRequest.builder().mediaId("m-1").build());

        assertThat(response.isSuggestionOnly()).isTrue();
        assertThat(response.getProvider()).isEqualTo("mock");
        assertThat(response.getMediaId()).isEqualTo("m-1");
        assertThat(response.getCategory()).isNotNull();
    }

    @Test
    void mapsCustomVisionClientResult() {
        VisionClient stub = new VisionClient() {
            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public VisionSuggestion suggest(String mediaId, byte[] imageBytes, String contentType) {
                return new VisionSuggestion(
                        ApplianceCategory.OVEN,
                        "Four",
                        IssueCode.OV_DOOR_SEAL,
                        "Joint usé",
                        0.77,
                        true,
                        "test"
                );
            }
        };
        VisionSuggestService service = new VisionSuggestService(mediaClient, stub);
        when(mediaClient.fetch("x"))
                .thenReturn(new MediaClient.MediaImage("x", new byte[]{1}, "image/jpeg"));

        VisionSuggestResponse response = service.suggest(VisionSuggestRequest.builder().mediaId("x").build());

        assertThat(response.getProvider()).isEqualTo("stub");
        assertThat(response.getCategory()).isEqualTo(ApplianceCategory.OVEN);
        assertThat(response.getSuggestedIssueCode()).isEqualTo(IssueCode.OV_DOOR_SEAL);
        assertThat(response.getConfidence()).isEqualTo(0.77);
        assertThat(response.isSuggestionOnly()).isTrue();
    }
}
