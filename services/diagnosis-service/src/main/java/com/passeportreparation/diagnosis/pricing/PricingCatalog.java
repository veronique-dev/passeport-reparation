package com.passeportreparation.diagnosis.pricing;

import com.passeportreparation.common.dto.IssueOptionDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Catalogue de pannes et prix (US-03, US-05).
 */
@Component
public class PricingCatalog {

    public List<IssueOptionDto> optionsFor(ApplianceCategory category) {
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

    public Optional<IssuePricing> find(ApplianceCategory category, IssueCode issueCode) {
        if (category == null || category == ApplianceCategory.UNSUPPORTED) {
            return Optional.empty();
        }
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
}
