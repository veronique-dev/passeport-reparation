package com.passeportreparation.diagnosis.pricing;

import com.passeportreparation.common.enums.RepairVerdict;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcule le verdict réparer / arbitrer / remplacer (US-06).
 */
public final class VerdictCalculator {

    private static final BigDecimal REPLACE_RATIO_THRESHOLD = new BigDecimal("0.70");

    private VerdictCalculator() {
    }

    /**
     * Si le milieu de fourchette dépasse 70 % du coût de remplacement → REPLACE.
     * Sinon conserve le verdict catalogue.
     */
    public static RepairVerdict refine(IssuePricing pricing) {
        BigDecimal mid = pricing.repairLow()
                .add(pricing.repairHigh())
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal ratio = mid.divide(pricing.replacementApprox(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(REPLACE_RATIO_THRESHOLD) > 0) {
            return RepairVerdict.REPLACE;
        }
        return pricing.defaultVerdict();
    }
}
