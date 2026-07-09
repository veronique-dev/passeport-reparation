package com.passeportreparation.diagnosis.service;

import com.passeportreparation.common.dto.VisionSuggestRequest;
import com.passeportreparation.common.dto.VisionSuggestResponse;
import com.passeportreparation.diagnosis.media.MediaClient;
import com.passeportreparation.diagnosis.vision.VisionClient;
import com.passeportreparation.diagnosis.vision.VisionSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VisionSuggestService {

    private final MediaClient mediaClient;
    private final VisionClient visionClient;

    public VisionSuggestResponse suggest(VisionSuggestRequest request) {
        MediaClient.MediaImage image = mediaClient.fetch(request.getMediaId());
        VisionSuggestion suggestion = visionClient.suggest(
                request.getMediaId(),
                image.bytes(),
                image.contentType()
        );

        return VisionSuggestResponse.builder()
                .mediaId(request.getMediaId())
                .category(suggestion.category())
                .applianceLabel(suggestion.applianceLabel())
                .suggestedIssueCode(suggestion.suggestedIssueCode())
                .probableIssue(suggestion.probableIssue())
                .confidence(suggestion.confidence())
                .supported(suggestion.supported())
                .provider(visionClient.providerName())
                .rationale(suggestion.rationale())
                .suggestionOnly(true)
                .build();
    }
}
