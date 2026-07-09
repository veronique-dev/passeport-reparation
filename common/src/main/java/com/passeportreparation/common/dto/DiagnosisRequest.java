package com.passeportreparation.common.dto;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    /** Catégorie confirmée par l'utilisateur (source de vérité MVP). */
    @NotNull
    private ApplianceCategory category;

    /** Type de panne sélectionné (obligatoire si catégorie supportée). */
    private IssueCode issueCode;

    private Double latitude;
    private Double longitude;
}
