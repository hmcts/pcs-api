package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;

@ExtendWith(MockitoExtension.class)
class EnforcementDecisionServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-10-17T09:30:00Z");
    private static final UUID CLAIM_ID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementNumberGenerator enforcementNumberGenerator;
    @Spy
    private EnforcementStatusTransitions enforcementStatusTransitions =
        new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC));
    @InjectMocks
    private EnforcementDecisionService underTest;

    @BeforeEach
    void setUp() {
        ClaimEntity claim = new ClaimEntity();
        claim.setId(CLAIM_ID);
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setClaims(List.of(claim));
        lenient().when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
    }

    @Test
    void shouldFindAnApplicationAwaitingReview() {
        givenAwaitingReview(SelectEnforcementType.WARRANT);

        assertThat(underTest.hasEnforcementAwaitingReview(CASE_REFERENCE)).isTrue();
    }

    @Test
    void shouldFindNoApplicationAwaitingReviewWhenNoneIs() {
        when(enforcementOrderRepository.findByClaimIdAndStatus(CLAIM_ID, AWAITING_REVIEW)).thenReturn(List.of());

        assertThat(underTest.hasEnforcementAwaitingReview(CASE_REFERENCE)).isFalse();
    }

    @Test
    void shouldFindNoApplicationAwaitingReviewWhenTheCaseHasNoClaim() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(new PcsCaseEntity());

        assertThat(underTest.hasEnforcementAwaitingReview(CASE_REFERENCE)).isFalse();
        verifyNoInteractions(enforcementOrderRepository);
    }

    @ParameterizedTest
    @CsvSource({
        "WARRANT, AWAITING_BAILIFF_ALLOCATION, WP-00000001",
        "WARRANT_OF_RESTITUTION, AWAITING_BAILIFF_ALLOCATION, WR-00000002",
        "WRIT, TRANSFERRED_TO_HIGH_COURT, HP-00000003",
        "WRIT_OF_RESTITUTION, TRANSFERRED_TO_HIGH_COURT, HR-00000004",
    })
    void shouldApproveTheApplication(SelectEnforcementType type, EnforcementStatus approvedStatus, String number) {
        EnforcementOrderEntity enforcement = givenAwaitingReview(type);
        when(enforcementNumberGenerator.next(type)).thenReturn(number);

        Optional<EnforcementDecisionOutcome> outcome = underTest.accept(CASE_REFERENCE);

        assertThat(outcome).contains(new EnforcementDecisionOutcome(type, EnforcementDecision.ACCEPT, number));
        assertThat(enforcement.getStatus()).isEqualTo(approvedStatus);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        assertThat(enforcement.getApprovedAt()).isEqualTo(NOW);
        assertThat(enforcement.getEnforcementNumber()).isEqualTo(number);
        assertThat(enforcement.getRejectionReason()).isNull();
        verify(enforcementStatusTransitions).transition(enforcement, approvedStatus);
        verify(enforcementOrderRepository).save(enforcement);
    }

    @Test
    void shouldTakeTheTypeFromTheOldEnforcementTypeFieldWhenTheNewOneIsMissing() {
        EnforcementOrderEntity enforcement = givenAwaitingReview(
            EnforcementOrder.builder().selectEnforcementType(SelectEnforcementType.WRIT).build());
        when(enforcementNumberGenerator.next(SelectEnforcementType.WRIT)).thenReturn("HP-00000009");

        underTest.accept(CASE_REFERENCE);

        assertThat(enforcement.getStatus()).isEqualTo(EnforcementStatus.TRANSFERRED_TO_HIGH_COURT);
    }

    @Test
    void shouldRefuseToApproveAnApplicationWithNoType() {
        givenAwaitingReview(EnforcementOrder.builder().build());

        assertThatThrownBy(() -> underTest.accept(CASE_REFERENCE)).isInstanceOf(IllegalStateException.class);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldApproveNothingWhenNoApplicationIsAwaitingReview() {
        when(enforcementOrderRepository.findByClaimIdAndStatus(CLAIM_ID, AWAITING_REVIEW)).thenReturn(List.of());

        assertThat(underTest.accept(CASE_REFERENCE)).isEmpty();
        verifyNoInteractions(enforcementNumberGenerator);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldRejectTheApplicationWithTheReasonAndNoNumber() {
        EnforcementOrderEntity enforcement = givenAwaitingReview(SelectEnforcementType.WARRANT);

        Optional<EnforcementDecisionOutcome> outcome =
            underTest.reject(CASE_REFERENCE, "  The order has been set aside  ");

        assertThat(outcome).contains(
            new EnforcementDecisionOutcome(SelectEnforcementType.WARRANT, EnforcementDecision.REJECT, null));
        assertThat(enforcement.getStatus()).isEqualTo(EnforcementStatus.REJECTED);
        assertThat(enforcement.getStatus().isLive()).isFalse();
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        assertThat(enforcement.getRejectionReason()).isEqualTo("The order has been set aside");
        assertThat(enforcement.getEnforcementNumber()).isNull();
        assertThat(enforcement.getApprovedAt()).isNull();
        verifyNoInteractions(enforcementNumberGenerator);
        verify(enforcementOrderRepository).save(enforcement);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRefuseToRejectWithoutAReason(String reason) {
        assertThatThrownBy(() -> underTest.reject(CASE_REFERENCE, reason))
            .isInstanceOf(EnforcementEventNotAvailableException.class)
            .hasMessage(EnforcementDecisionService.NO_REJECTION_REASON_MESSAGE);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldRejectNothingWhenNoApplicationIsAwaitingReview() {
        when(enforcementOrderRepository.findByClaimIdAndStatus(CLAIM_ID, AWAITING_REVIEW)).thenReturn(List.of());

        assertThat(underTest.reject(CASE_REFERENCE, "reason")).isEmpty();
        verify(enforcementOrderRepository, never()).save(any());
    }

    private EnforcementOrderEntity givenAwaitingReview(SelectEnforcementType type) {
        return givenAwaitingReview(buildEnforcementOrderWithSpecifiedType(type));
    }

    private EnforcementOrderEntity givenAwaitingReview(EnforcementOrder enforcementOrder) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        enforcement.setEnforcementOrder(enforcementOrder);
        enforcement.setStatus(AWAITING_REVIEW);
        when(enforcementOrderRepository.findByClaimIdAndStatus(CLAIM_ID, AWAITING_REVIEW))
            .thenReturn(List.of(enforcement));
        return enforcement;
    }
}
