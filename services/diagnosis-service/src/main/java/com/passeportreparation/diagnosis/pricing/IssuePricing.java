package com.passeportreparation.diagnosis.pricing;

import com.passeportreparation.common.dto.CostEstimateDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import com.passeportreparation.common.enums.RepairVerdict;

import java.math.BigDecimal;
import java.util.List;

public record IssuePricing(
        IssueCode code,
        ApplianceCategory category,
        String applianceLabel,
        String issueLabel,
        BigDecimal repairLow,
        BigDecimal repairHigh,
        BigDecimal replacementApprox,
        RepairVerdict defaultVerdict
) {
    public CostEstimateDto toEstimate() {
        return CostEstimateDto.builder()
                .repairLow(repairLow)
                .repairHigh(repairHigh)
                .replacementApprox(replacementApprox)
                .currency("EUR")
                .build();
    }

    public static final List<IssuePricing> CATALOG = List.of(
            // Lave-linge
            entry(IssueCode.WM_DRAIN_PUMP, ApplianceCategory.WASHING_MACHINE, "Lave-linge",
                    "Ne vidange plus (pompe / filtre)", 80, 180, 450, RepairVerdict.REPAIR),
            entry(IssueCode.WM_DOOR_LOCK, ApplianceCategory.WASHING_MACHINE, "Lave-linge",
                    "Porte qui ne se verrouille plus", 70, 150, 450, RepairVerdict.REPAIR),
            entry(IssueCode.WM_NO_SPIN, ApplianceCategory.WASHING_MACHINE, "Lave-linge",
                    "Ne essore plus / moteur", 100, 250, 450, RepairVerdict.ARBITRATE),
            entry(IssueCode.WM_ELECTRONIC_BOARD, ApplianceCategory.WASHING_MACHINE, "Lave-linge",
                    "Carte électronique défaillante", 160, 350, 450, RepairVerdict.ARBITRATE),
            entry(IssueCode.WM_UNKNOWN, ApplianceCategory.WASHING_MACHINE, "Lave-linge",
                    "Panne non identifiée", 90, 260, 450, RepairVerdict.ARBITRATE),

            // Lave-vaisselle
            entry(IssueCode.DW_HEATING, ApplianceCategory.DISHWASHER, "Lave-vaisselle",
                    "Ne chauffe plus (résistance)", 90, 220, 500, RepairVerdict.REPAIR),
            entry(IssueCode.DW_DRAIN, ApplianceCategory.DISHWASHER, "Lave-vaisselle",
                    "Ne vidange plus", 80, 180, 500, RepairVerdict.REPAIR),
            entry(IssueCode.DW_SPRAY_ARM, ApplianceCategory.DISHWASHER, "Lave-vaisselle",
                    "Bras de lavage / mauvaise répartition", 60, 140, 500, RepairVerdict.REPAIR),
            entry(IssueCode.DW_ELECTRONIC_BOARD, ApplianceCategory.DISHWASHER, "Lave-vaisselle",
                    "Carte électronique défaillante", 150, 320, 500, RepairVerdict.ARBITRATE),
            entry(IssueCode.DW_UNKNOWN, ApplianceCategory.DISHWASHER, "Lave-vaisselle",
                    "Panne non identifiée", 90, 280, 500, RepairVerdict.ARBITRATE),

            // Four
            entry(IssueCode.OV_THERMOSTAT, ApplianceCategory.OVEN, "Four",
                    "Ne monte plus en température (thermostat)", 70, 180, 400, RepairVerdict.REPAIR),
            entry(IssueCode.OV_HEATING_ELEMENT, ApplianceCategory.OVEN, "Four",
                    "Résistance grill / sole", 80, 200, 400, RepairVerdict.REPAIR),
            entry(IssueCode.OV_DOOR_SEAL, ApplianceCategory.OVEN, "Four",
                    "Joint de porte usé", 50, 120, 400, RepairVerdict.REPAIR),
            entry(IssueCode.OV_ELECTRONIC_BOARD, ApplianceCategory.OVEN, "Four",
                    "Carte électronique / affichage", 140, 300, 400, RepairVerdict.ARBITRATE),
            entry(IssueCode.OV_UNKNOWN, ApplianceCategory.OVEN, "Four",
                    "Panne non identifiée", 80, 240, 400, RepairVerdict.ARBITRATE)
    );

    private static IssuePricing entry(
            IssueCode code,
            ApplianceCategory category,
            String applianceLabel,
            String issueLabel,
            int low,
            int high,
            int replacement,
            RepairVerdict verdict
    ) {
        return new IssuePricing(
                code,
                category,
                applianceLabel,
                issueLabel,
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(replacement),
                verdict
        );
    }
}
