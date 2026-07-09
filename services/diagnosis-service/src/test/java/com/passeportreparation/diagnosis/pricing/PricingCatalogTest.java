package com.passeportreparation.diagnosis.pricing;

import com.passeportreparation.common.dto.IssueOptionDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCatalogTest {

    private PricingCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new PricingCatalog();
    }

    @Test
    void us03_listsIssuesForOven() {
        List<IssueOptionDto> issues = catalog.optionsFor(ApplianceCategory.OVEN);

        assertThat(issues).isNotEmpty();
        assertThat(issues).allMatch(i -> i.getCategory() == ApplianceCategory.OVEN);
        assertThat(issues).extracting(IssueOptionDto::getCode)
                .contains(IssueCode.OV_DOOR_SEAL, IssueCode.OV_UNKNOWN);
    }

    @Test
    void us03_returnsEmptyForUnsupported() {
        assertThat(catalog.optionsFor(ApplianceCategory.UNSUPPORTED)).isEmpty();
        assertThat(catalog.optionsFor(null)).isEmpty();
    }

    @Test
    void us05_doorSealCheaperThanElectronicBoard() {
        IssuePricing seal = catalog.find(ApplianceCategory.OVEN, IssueCode.OV_DOOR_SEAL).orElseThrow();
        IssuePricing board = catalog.find(ApplianceCategory.OVEN, IssueCode.OV_ELECTRONIC_BOARD).orElseThrow();

        assertThat(seal.repairHigh()).isLessThan(board.repairHigh());
        assertThat(seal.toEstimate().getCurrency()).isEqualTo("EUR");
    }

    @Test
    void us05_fallsBackToUnknownWhenIssueCodeMissing() {
        IssuePricing pricing = catalog.find(ApplianceCategory.WASHING_MACHINE, null).orElseThrow();
        assertThat(pricing.code()).isEqualTo(IssueCode.WM_UNKNOWN);
    }

    @Test
    void us05_rejectsMismatchedIssueAndCategory() {
        assertThat(catalog.find(ApplianceCategory.OVEN, IssueCode.WM_DRAIN_PUMP)).isEmpty();
    }
}
