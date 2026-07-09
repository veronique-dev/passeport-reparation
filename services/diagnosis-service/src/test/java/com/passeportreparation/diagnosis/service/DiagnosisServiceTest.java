package com.passeportreparation.diagnosis.service;

import com.passeportreparation.common.dto.DiagnosisRequest;
import com.passeportreparation.common.dto.DiagnosisResponse;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import com.passeportreparation.common.enums.RepairVerdict;
import com.passeportreparation.diagnosis.entity.Diagnosis;
import com.passeportreparation.diagnosis.pricing.PricingCatalog;
import com.passeportreparation.diagnosis.repository.DiagnosisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock
    private DiagnosisRepository repository;

    private DiagnosisService service;

    @BeforeEach
    void setUp() {
        service = new DiagnosisService(repository, new PricingCatalog());
        lenient().when(repository.save(any(Diagnosis.class))).thenAnswer(invocation -> {
            Diagnosis d = invocation.getArgument(0);
            if (d.getId() == null) {
                d.setId(UUID.randomUUID());
            }
            return d;
        });
    }

    @Test
    void us04_unsupportedReturnsNoEstimate() {
        DiagnosisRequest request = DiagnosisRequest.builder()
                .mediaId("media-1")
                .category(ApplianceCategory.UNSUPPORTED)
                .build();

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.isSupported()).isFalse();
        assertThat(response.getEstimate()).isNull();
        assertThat(response.getVerdict()).isNull();
        assertThat(response.getIssueCode()).isEqualTo(IssueCode.UNSUPPORTED_OTHER);
        assertThat(response.isUserConfirmed()).isTrue();
        assertThat(response.getDisclaimer()).contains("indicative");
    }

    @Test
    void us05_returnsCostEstimateForDrainPump() {
        DiagnosisRequest request = DiagnosisRequest.builder()
                .mediaId("media-2")
                .category(ApplianceCategory.WASHING_MACHINE)
                .issueCode(IssueCode.WM_DRAIN_PUMP)
                .build();

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.isSupported()).isTrue();
        assertThat(response.getApplianceLabel()).isEqualTo("Lave-linge");
        assertThat(response.getEstimate()).isNotNull();
        assertThat(response.getEstimate().getRepairLow()).isEqualByComparingTo("80");
        assertThat(response.getEstimate().getRepairHigh()).isEqualByComparingTo("180");
        assertThat(response.getEstimate().getReplacementApprox()).isEqualByComparingTo("450");
        assertThat(response.getEstimate().getCurrency()).isEqualTo("EUR");
        assertThat(response.getVerdict()).isEqualTo(RepairVerdict.REPAIR);
    }

    @Test
    void us06_and_us07_persistVerdictAndDisclaimer() {
        DiagnosisRequest request = DiagnosisRequest.builder()
                .mediaId("media-3")
                .category(ApplianceCategory.OVEN)
                .issueCode(IssueCode.OV_DOOR_SEAL)
                .build();

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.getVerdict()).isEqualTo(RepairVerdict.REPAIR);
        assertThat(response.getDisclaimer()).isEqualTo(DiagnosisService.DISCLAIMER);

        ArgumentCaptor<Diagnosis> captor = ArgumentCaptor.forClass(Diagnosis.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIssueCode()).isEqualTo(IssueCode.OV_DOOR_SEAL);
        assertThat(captor.getValue().isUserConfirmed()).isTrue();
    }

    @Test
    void us05_rejectsInvalidIssueForCategory() {
        DiagnosisRequest request = DiagnosisRequest.builder()
                .mediaId("media-4")
                .category(ApplianceCategory.OVEN)
                .issueCode(IssueCode.WM_DRAIN_PUMP)
                .build();

        assertThatThrownBy(() -> service.diagnose(request))
                .isInstanceOf(InvalidDiagnosisRequestException.class)
                .hasMessageContaining("Type de panne invalide");
    }

    @Test
    void us03_listsIssuesViaService() {
        assertThat(service.listIssues(ApplianceCategory.DISHWASHER))
                .extracting(i -> i.getCode())
                .contains(IssueCode.DW_HEATING, IssueCode.DW_UNKNOWN);
    }
}
