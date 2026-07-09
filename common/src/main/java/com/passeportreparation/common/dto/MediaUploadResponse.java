package com.passeportreparation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {
    private String mediaId;
    private String contentType;
    private long sizeBytes;
    private String url;
}
