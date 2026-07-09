package com.passeportreparation.repairer.service;

import com.passeportreparation.common.dto.RepairerDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.repairer.entity.Repairer;
import com.passeportreparation.repairer.repository.RepairerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairerServiceTest {

    @Mock
    private RepairerRepository repository;

    private RepairerService service;

    @BeforeEach
    void setUp() {
        service = new RepairerService(repository);
    }

    @Test
    void us08_filtersByCategoryAndMapsContactFields() {
        Repairer repairer = Repairer.builder()
                .id(UUID.randomUUID())
                .name("ElectroFix Lyon")
                .city("Lyon")
                .phone("+33478000001")
                .email("contact@electrofix-lyon.fr")
                .whatsapp("+33612000001")
                .latitude(45.7640)
                .longitude(4.8357)
                .categories(EnumSet.of(ApplianceCategory.WASHING_MACHINE, ApplianceCategory.DISHWASHER))
                .active(true)
                .build();

        when(repository.search(ApplianceCategory.WASHING_MACHINE, "Lyon"))
                .thenReturn(List.of(repairer));

        List<RepairerDto> result = service.findNearby(ApplianceCategory.WASHING_MACHINE, "Lyon", null, null);

        assertThat(result).hasSize(1);
        RepairerDto dto = result.get(0);
        assertThat(dto.getName()).isEqualTo("ElectroFix Lyon");
        assertThat(dto.getPhone()).isEqualTo("+33478000001");
        assertThat(dto.getEmail()).isEqualTo("contact@electrofix-lyon.fr");
        assertThat(dto.getWhatsapp()).isEqualTo("+33612000001");
        assertThat(dto.getCategories()).contains(ApplianceCategory.WASHING_MACHINE);
        assertThat(dto.getDistanceKm()).isNull();
    }

    @Test
    void us08_returnsEmptyListWhenNoRepairerMatches() {
        when(repository.search(ApplianceCategory.OVEN, "Lyon")).thenReturn(List.of());

        assertThat(service.findNearby(ApplianceCategory.OVEN, "Lyon", null, null)).isEmpty();
    }

    @Test
    void us08_sortsByDistanceWhenCoordinatesProvided() {
        Repairer near = Repairer.builder()
                .id(UUID.randomUUID())
                .name("Proche")
                .city("Lyon")
                .latitude(45.7600)
                .longitude(4.8400)
                .categories(EnumSet.of(ApplianceCategory.OVEN))
                .active(true)
                .build();
        Repairer far = Repairer.builder()
                .id(UUID.randomUUID())
                .name("Loin")
                .city("Lyon")
                .latitude(45.8000)
                .longitude(4.9000)
                .categories(EnumSet.of(ApplianceCategory.OVEN))
                .active(true)
                .build();

        when(repository.search(ApplianceCategory.OVEN, "Lyon")).thenReturn(List.of(far, near));

        List<RepairerDto> result = service.findNearby(ApplianceCategory.OVEN, "Lyon", 45.7600, 4.8400);

        assertThat(result).extracting(RepairerDto::getName).containsExactly("Proche", "Loin");
        assertThat(result.get(0).getDistanceKm()).isNotNull();
        assertThat(result.get(0).getDistanceKm()).isLessThan(result.get(1).getDistanceKm());
    }
}
