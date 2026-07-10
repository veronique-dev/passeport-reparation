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

import java.time.Instant;
import java.util.List;
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
            if (d.getCreatedAt() == null) {
                d.setCreatedAt(java.time.Instant.now());
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

        DiagnosisResponse response = service.diagnose(request, null);

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

        DiagnosisResponse response = service.diagnose(request, null);

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

        DiagnosisResponse response = service.diagnose(request, null);

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

        assertThatThrownBy(() -> service.diagnose(request, null))
                .isInstanceOf(InvalidDiagnosisRequestException.class)
                .hasMessageContaining("Type de panne invalide");
    }

    void us03_listsIssuesViaService() {
        assertThat(service.listIssues(ApplianceCategory.DISHWASHER))
                .extracting(i -> i.getCode())
                .contains(IssueCode.DW_HEATING, IssueCode.DW_UNKNOWN);
    }

    @Test
    void us12_claimsAnonymousDiagnosis() {
        UUID diagnosisId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Diagnosis entity = Diagnosis.builder()
                .id(diagnosisId)
                .mediaId("m")
                .category(ApplianceCategory.OVEN)
                .applianceLabel("Four")
                .issueCode(IssueCode.OV_DOOR_SEAL)
                .probableIssue("Joint")
                .supported(true)
                .userConfirmed(true)
                .userId(null)
                .build();
        when(repository.findById(diagnosisId)).thenReturn(java.util.Optional.of(entity));

        DiagnosisResponse response = service.claim(diagnosisId, userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(entity.getUserId()).isEqualTo(userId);
        verify(repository).save(entity);
    }

    @Test
    void us12_claimIsIdempotentForSameUser() {
        UUID diagnosisId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Diagnosis entity = Diagnosis.builder()
                .id(diagnosisId)
                .mediaId("m")
                .category(ApplianceCategory.OVEN)
                .applianceLabel("Four")
                .supported(false)
                .userConfirmed(true)
                .userId(userId)
                .build();
        when(repository.findById(diagnosisId)).thenReturn(java.util.Optional.of(entity));

        DiagnosisResponse response = service.claim(diagnosisId, userId);

        assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    void us12_claimRejectsOtherOwner() {
        UUID diagnosisId = UUID.randomUUID();
        Diagnosis entity = Diagnosis.builder()
                .id(diagnosisId)
                .mediaId("m")
                .category(ApplianceCategory.OVEN)
                .applianceLabel("Four")
                .supported(false)
                .userConfirmed(true)
                .userId(UUID.randomUUID())
                .build();
        when(repository.findById(diagnosisId)).thenReturn(java.util.Optional.of(entity));

        assertThatThrownBy(() -> service.claim(diagnosisId, UUID.randomUUID()))
                .isInstanceOf(DiagnosisClaimConflictException.class);
    }

    @Test
    void us13_listMineReturnsCreatedAtNewestFirst() {
        UUID userId = UUID.randomUUID();
        Instant older = Instant.parse("2026-07-01T10:00:00Z");
        Instant newer = Instant.parse("2026-07-10T15:30:00Z");

        Diagnosis first = Diagnosis.builder()
                .id(UUID.randomUUID())
                .mediaId("m-new")
                .category(ApplianceCategory.OVEN)
                .applianceLabel("Four")
                .probableIssue("Joint")
                .supported(true)
                .userConfirmed(true)
                .userId(userId)
                .createdAt(newer)
                .build();
        Diagnosis second = Diagnosis.builder()
                .id(UUID.randomUUID())
                .mediaId("m-old")
                .category(ApplianceCategory.WASHING_MACHINE)
                .applianceLabel("Lave-linge")
                .probableIssue("Pompe")
                .supported(true)
                .userConfirmed(true)
                .userId(userId)
                .createdAt(older)
                .build();

        when(repository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(first, second));

        List<DiagnosisResponse> mine = service.listMine(userId);

        assertThat(mine).hasSize(2);
        assertThat(mine.get(0).getCreatedAt()).isEqualTo(newer);
        assertThat(mine.get(1).getCreatedAt()).isEqualTo(older);
        assertThat(mine.get(0).getApplianceLabel()).isEqualTo("Four");
        verify(repository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void us05_diagnoseExposesCreatedAt() {
        DiagnosisRequest request = DiagnosisRequest.builder()
                .mediaId("media-1")
                .category(ApplianceCategory.OVEN)
                .issueCode(IssueCode.OV_DOOR_SEAL)
                .build();

        DiagnosisResponse response = service.diagnose(request, null);

        assertThat(response.getCreatedAt()).isNotNull();
    }
}
