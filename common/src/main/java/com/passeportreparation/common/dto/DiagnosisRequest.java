package com.passeportreparation.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRequest {

    @NotBlank
    private String mediaId;

    private Double latitude;
    private Double longitude;
}
