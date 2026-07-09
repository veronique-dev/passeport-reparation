package com.passeportreparation.diagnosis.pricing;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import com.passeportreparation.common.enums.RepairVerdict;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VerdictCalculatorTest {

    @Test
    void us06_keepsCatalogVerdictWhenRatioBelowThreshold() {
        IssuePricing pricing = IssuePricing.CATALOG.stream()
                .filter(p -> p.code() == IssueCode.OV_DOOR_SEAL)
                .findFirst()
                .orElseThrow();

        // mid = 85 / 400 = 0.2125 → conserve REPAIR
        assertThat(VerdictCalculator.refine(pricing)).isEqualTo(RepairVerdict.REPAIR);
    }

    @Test
    void us06_forcesReplaceWhenMidExceedsSeventyPercentOfReplacement() {
        IssuePricing expensive = new IssuePricing(
                IssueCode.WM_ELECTRONIC_BOARD,
                ApplianceCategory.WASHING_MACHINE,
                "Lave-linge",
                "Carte très coûteuse",
                BigDecimal.valueOf(300),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(400),
                RepairVerdict.ARBITRATE
        );
        // mid = 350 / 400 = 0.875 > 0.70 → REPLACE
        assertThat(VerdictCalculator.refine(expensive)).isEqualTo(RepairVerdict.REPLACE);
    }

    @Test
    void us06_keepsArbitrateForElectronicBoardInCatalog() {
        IssuePricing pricing = IssuePricing.CATALOG.stream()
                .filter(p -> p.code() == IssueCode.OV_ELECTRONIC_BOARD)
                .findFirst()
                .orElseThrow();

        assertThat(VerdictCalculator.refine(pricing)).isEqualTo(RepairVerdict.ARBITRATE);
    }
}
