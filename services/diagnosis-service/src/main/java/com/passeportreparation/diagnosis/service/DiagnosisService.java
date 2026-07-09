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
import com.passeportreparation.diagnosis.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private static final String DISCLAIMER =
            "Estimation indicative basée sur le type de panne déclaré — ce n'est pas un devis. Un réparateur confirmera sur place.";

    private final DiagnosisRepository repository;

    public List<IssueOptionDto> listIssues(ApplianceCategory category) {
        if (category == null || category == ApplianceCategory.UNSUPPORTED) {
            return List.of();
        }
        return IssuePricing.CATALOG.stream()
                .filter(item -> item.category() == category)
                .map(item -> IssueOptionDto.builder()
                        .code(item.code())
                        .label(item.issueLabel())
                        .category(item.category())
                        .build())
                .toList();
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

        IssuePricing pricing = resolvePricing(category, request.getIssueCode())
                .orElseThrow(() -> new InvalidDiagnosisRequestException(
                        "Type de panne invalide pour la catégorie " + category));

        CostEstimateDto estimate = pricing.toEstimate();
        RepairVerdict verdict = refineVerdict(pricing);

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
    public DiagnosisResponse getById(java.util.UUID id) {
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

    private static Optional<IssuePricing> resolvePricing(ApplianceCategory category, IssueCode issueCode) {
        if (issueCode == null) {
            return IssuePricing.CATALOG.stream()
                    .filter(item -> item.category() == category)
                    .filter(item -> item.code().name().endsWith("_UNKNOWN"))
                    .findFirst();
        }
        return IssuePricing.CATALOG.stream()
                .filter(item -> item.code() == issueCode && item.category() == category)
                .findFirst();
    }

    /**
     * Affinage : si le milieu de fourchette dépasse 70% du remplacement → REPLACE,
     * sinon on garde le verdict catalogue (souvent plus pertinent que le seul ratio).
     */
    private static RepairVerdict refineVerdict(IssuePricing pricing) {
        BigDecimal mid = pricing.repairLow()
                .add(pricing.repairHigh())
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal ratio = mid.divide(pricing.replacementApprox(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.70")) > 0) {
            return RepairVerdict.REPLACE;
        }
        return pricing.defaultVerdict();
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
