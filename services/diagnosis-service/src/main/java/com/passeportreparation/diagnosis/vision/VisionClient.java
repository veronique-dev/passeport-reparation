package com.passeportreparation.diagnosis.vision;

public interface VisionClient {

    String providerName();

    VisionSuggestion suggest(String mediaId, byte[] imageBytes, String contentType);
}
