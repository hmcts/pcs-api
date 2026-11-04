package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskLevelServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-02-01T16:20:00Z");

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    private RiskLevelService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RiskLevelService(currentEnforcementService, enforcementOrderRepository,
                                         Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldSaveAHighRiskLevelWithItsReason() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(EnforcementStatus.AWAITING_SCHEDULING);
        enforcement.setEnforcementNumber("WP-00000001");

        EnforcementChange<String> set = underTest.setRiskLevel(CASE_REFERENCE, RiskLevel.HIGH,
                                                               "Known history of violence");

        assertThat(set.refused()).isFalse();
        assertThat(set.change()).isEqualTo("WP-00000001");
        assertThat(enforcement.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(enforcement.getRiskLevelReason()).isEqualTo("Known history of violence");
        assertThat(enforcement.getRiskLevelSetAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(enforcement);
    }

    @Test
    void shouldRemoveTheReasonWhenTheLevelIsResetToMedium() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(EnforcementStatus.CLAIMANT_CONFIRMED);
        enforcement.setRiskLevel(RiskLevel.HIGH);
        enforcement.setRiskLevelReason("Dog on site");

        underTest.setRiskLevel(CASE_REFERENCE, RiskLevel.MEDIUM, "ignored");

        assertThat(enforcement.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(enforcement.getRiskLevelReason()).isNull();
        verify(enforcementOrderRepository).save(enforcement);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void shouldRefuseAHighLevelWithoutAReason(String reason) {
        assertThatThrownBy(() -> underTest.setRiskLevel(CASE_REFERENCE, RiskLevel.HIGH, reason))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(currentEnforcementService, enforcementOrderRepository);
    }

    @Test
    void shouldRefuseAMissingLevel() {
        assertThatThrownBy(() -> underTest.setRiskLevel(CASE_REFERENCE, null, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRefuseWhenTheClaimHasNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getEnforcementForRiskLevel(CASE_REFERENCE))
            .isInstanceOf(NoEnforcementException.class);
    }

    @Test
    void shouldRefuseAWarrantAwaitingReview() {
        givenCurrentEnforcement(EnforcementStatus.AWAITING_REVIEW);

        EnforcementChange<String> set = underTest.setRiskLevel(CASE_REFERENCE, RiskLevel.MEDIUM, null);

        assertThat(set.errors()).containsExactly(RiskLevelService.NOT_AVAILABLE_MESSAGE);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldRefuseRatherThanThrowWhenTheClaimHasNoEnforcementOnSubmit() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        EnforcementChange<String> set = underTest.setRiskLevel(CASE_REFERENCE, RiskLevel.MEDIUM, null);

        assertThat(set.errors()).containsExactly(NoEnforcementException.MESSAGE);
    }

    @Test
    void shouldFindTheCurrentEnforcementForTheCaseView() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        when(currentEnforcementService.findCurrentEnforcement(pcsCase)).thenReturn(Optional.of(enforcement));

        assertThat(underTest.findCurrentEnforcement(pcsCase)).contains(enforcement);
    }

    private EnforcementOrderEntity givenCurrentEnforcement(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = EnforcementOrderEntity.builder()
            .warrantDetails(new WarrantEntity())
            .status(status)
            .build();
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
        return enforcement;
    }
}
