package com.passeportreparation.diagnosis.service;

import com.passeportreparation.common.dto.CostEstimateDto;
import com.passeportreparation.common.dto.DiagnosisRequest;
import com.passeportreparation.common.dto.DiagnosisResponse;
import com.passeportreparation.common.dto.IssueOptionDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import com.passeportreparation.common.enums.RepairVerdict;
import com.passeportreparation.diagnosis.entity.Diagnosis;
import com.passeportreparation.diagnosis.pricing.IssuePricing;
import com.passeportreparation.diagnosis.pricing.PricingCatalog;
import com.passeportreparation.diagnosis.pricing.VerdictCalculator;
import com.passeportreparation.diagnosis.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implémentation métier des US-03 → US-07 :
 * liste des pannes, estimation par grille, verdict, hors périmètre, disclaimer.
 */
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    static final String DISCLAIMER =
            "Estimation indicative basée sur le type de panne déclaré — ce n'est pas un devis. Un réparateur confirmera sur place.";

    private final DiagnosisRepository repository;
    private final PricingCatalog pricingCatalog;

    public List<IssueOptionDto> listIssues(ApplianceCategory category) {
        return pricingCatalog.optionsFor(category);
    }

    @Transactional
    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        ApplianceCategory category = request.getCategory();
        boolean supported = category != null && category != ApplianceCategory.UNSUPPORTED;

        if (!supported) {
            Diagnosis entity = Diagnosis.builder()
                    .mediaId(request.getMediaId())
                    .category(ApplianceCategory.UNSUPPORTED)
                    .applianceLabel("Appareil hors périmètre")
                    .issueCode(IssueCode.UNSUPPORTED_OTHER)
                    .probableIssue("Catégorie non supportée pour l’instant (lave-linge, lave-vaisselle, four uniquement)")
                    .confidence(1.0)
                    .verdict(null)
                    .supported(false)
                    .userConfirmed(true)
                    .build();
            return toResponse(repository.save(entity), null);
        }

        IssuePricing pricing = pricingCatalog.find(category, request.getIssueCode())
                .orElseThrow(() -> new InvalidDiagnosisRequestException(
                        "Type de panne invalide pour la catégorie " + category));

        CostEstimateDto estimate = pricing.toEstimate();
        RepairVerdict verdict = VerdictCalculator.refine(pricing);

        Diagnosis entity = Diagnosis.builder()
                .mediaId(request.getMediaId())
                .category(category)
                .applianceLabel(pricing.applianceLabel())
                .issueCode(pricing.code())
                .probableIssue(pricing.issueLabel())
                .confidence(1.0)
                .repairLow(estimate.getRepairLow())
                .repairHigh(estimate.getRepairHigh())
                .replacementApprox(estimate.getReplacementApprox())
                .verdict(verdict)
                .supported(true)
                .userConfirmed(true)
                .build();

        return toResponse(repository.save(entity), estimate);
    }

    @Transactional(readOnly = true)
    public DiagnosisResponse getById(UUID id) {
        Diagnosis entity = repository.findById(id)
                .orElseThrow(() -> new DiagnosisNotFoundException(id));
        CostEstimateDto estimate = null;
        if (entity.isSupported() && entity.getRepairLow() != null) {
            estimate = CostEstimateDto.builder()
                    .repairLow(entity.getRepairLow())
                    .repairHigh(entity.getRepairHigh())
                    .replacementApprox(entity.getReplacementApprox())
                    .currency("EUR")
                    .build();
        }
        return toResponse(entity, estimate);
    }

    private static DiagnosisResponse toResponse(Diagnosis entity, CostEstimateDto estimate) {
        return DiagnosisResponse.builder()
                .id(entity.getId())
                .mediaId(entity.getMediaId())
                .category(entity.getCategory())
                .applianceLabel(entity.getApplianceLabel())
                .issueCode(entity.getIssueCode())
                .probableIssue(entity.getProbableIssue())
                .confidence(entity.getConfidence())
                .estimate(estimate)
                .verdict(entity.getVerdict())
                .disclaimer(DISCLAIMER)
                .supported(entity.isSupported())
                .userConfirmed(entity.isUserConfirmed())
                .build();
    }
}
