package com.passeportreparation.diagnosis.service;

import com.passeportreparation.common.dto.CostEstimateDto;
import com.passeportreparation.common.dto.DiagnosisRequest;
import com.passeportreparation.common.dto.DiagnosisResponse;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.RepairVerdict;
import com.passeportreparation.diagnosis.entity.Diagnosis;
import com.passeportreparation.diagnosis.repository.DiagnosisRepository;
import com.passeportreparation.diagnosis.vision.VisionClient;
import com.passeportreparation.diagnosis.vision.VisionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private static final String DISCLAIMER =
            "Estimation indicative uniquement — ce n'est pas un devis. Un réparateur confirmera sur place.";

    private static final Map<ApplianceCategory, CostEstimateDto> ESTIMATES = new EnumMap<>(ApplianceCategory.class);

    static {
        ESTIMATES.put(ApplianceCategory.WASHING_MACHINE, CostEstimateDto.builder()
                .repairLow(bd(80)).repairHigh(bd(220)).replacementApprox(bd(450)).currency("EUR").build());
        ESTIMATES.put(ApplianceCategory.DISHWASHER, CostEstimateDto.builder()
                .repairLow(bd(90)).repairHigh(bd(250)).replacementApprox(bd(500)).currency("EUR").build());
        ESTIMATES.put(ApplianceCategory.OVEN, CostEstimateDto.builder()
                .repairLow(bd(70)).repairHigh(bd(200)).replacementApprox(bd(400)).currency("EUR").build());
    }

    private final DiagnosisRepository repository;
    private final VisionClient visionClient;

    @Transactional
    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        VisionResult vision = visionClient.analyze(request.getMediaId());

        CostEstimateDto estimate = null;
        RepairVerdict verdict = null;

        if (vision.supported()) {
            estimate = ESTIMATES.get(vision.category());
            verdict = computeVerdict(estimate);
        }

        Diagnosis entity = Diagnosis.builder()
                .mediaId(request.getMediaId())
                .category(vision.category())
                .applianceLabel(vision.applianceLabel())
                .probableIssue(vision.probableIssue())
                .confidence(vision.confidence())
                .repairLow(estimate != null ? estimate.getRepairLow() : null)
                .repairHigh(estimate != null ? estimate.getRepairHigh() : null)
                .replacementApprox(estimate != null ? estimate.getReplacementApprox() : null)
                .verdict(verdict)
                .supported(vision.supported())
                .build();

        entity = repository.save(entity);
        return toResponse(entity, estimate);
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

    private static RepairVerdict computeVerdict(CostEstimateDto estimate) {
        BigDecimal mid = estimate.getRepairLow()
                .add(estimate.getRepairHigh())
                .divide(bd(2), 2, RoundingMode.HALF_UP);
        BigDecimal ratio = mid.divide(estimate.getReplacementApprox(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(bd("0.40")) <= 0) {
            return RepairVerdict.REPAIR;
        }
        if (ratio.compareTo(bd("0.70")) <= 0) {
            return RepairVerdict.ARBITRATE;
        }
        return RepairVerdict.REPLACE;
    }

    private static DiagnosisResponse toResponse(Diagnosis entity, CostEstimateDto estimate) {
        return DiagnosisResponse.builder()
                .id(entity.getId())
                .mediaId(entity.getMediaId())
                .category(entity.getCategory())
                .applianceLabel(entity.getApplianceLabel())
                .probableIssue(entity.getProbableIssue())
                .confidence(entity.getConfidence())
                .estimate(estimate)
                .verdict(entity.getVerdict())
                .disclaimer(DISCLAIMER)
                .supported(entity.isSupported())
                .build();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
