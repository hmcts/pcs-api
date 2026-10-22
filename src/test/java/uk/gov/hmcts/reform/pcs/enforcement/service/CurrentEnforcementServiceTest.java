package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentEnforcementServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final Instant EARLIER = Instant.parse("2026-02-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-01T10:00:00Z");

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    @InjectMocks
    private CurrentEnforcementService underTest;

    @Test
    void shouldPreferTheLiveEnforcementOverAMoreRecentlyClosedOne() {
        EnforcementOrderEntity live = enforcement(EnforcementStatus.AWAITING_SCHEDULING, EARLIER);
        EnforcementOrderEntity closed = enforcement(EnforcementStatus.CLOSED, LATER);
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(closed, live));

        assertThat(underTest.findCurrentEnforcement(caseWithClaim())).contains(live);
    }

    @Test
    void shouldFallBackToTheMostRecentEnforcementWhenNoneIsLive() {
        EnforcementOrderEntity olderClosed = enforcement(EnforcementStatus.CLOSED, EARLIER);
        EnforcementOrderEntity newerRejected = enforcement(EnforcementStatus.REJECTED, LATER);
        EnforcementOrderEntity unknownTime = enforcement(EnforcementStatus.CLOSED, null);
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID))
            .thenReturn(List.of(unknownTime, olderClosed, newerRejected));

        assertThat(underTest.findCurrentEnforcement(caseWithClaim())).contains(newerRejected);
    }

    @Test
    void shouldIgnoreAnEnforcementWithNoStatus() {
        EnforcementOrderEntity noStatus = enforcement(null, LATER);
        EnforcementOrderEntity live = enforcement(EnforcementStatus.AWAITING_PAYMENT, EARLIER);
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(noStatus, live));

        assertThat(underTest.findCurrentEnforcement(caseWithClaim())).contains(live);
        assertThat(underTest.findEnforcementsCurrentFirst(caseWithClaim())).containsExactly(live);
    }

    @Test
    void shouldFindNothingWhenNoEnforcementHasAStatus() {
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(enforcement(null, LATER)));

        assertThat(underTest.findCurrentEnforcement(caseWithClaim())).isEmpty();
    }

    @Test
    void shouldListEveryEnforcementCurrentFirst() {
        EnforcementOrderEntity live = enforcement(EnforcementStatus.AWAITING_SCHEDULING, EARLIER);
        EnforcementOrderEntity laterRejected = enforcement(EnforcementStatus.REJECTED, LATER);
        EnforcementOrderEntity olderClosed = enforcement(EnforcementStatus.CLOSED, EARLIER);
        EnforcementOrderEntity unknownTime = enforcement(EnforcementStatus.CLOSED, null);
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID))
            .thenReturn(List.of(unknownTime, olderClosed, live, laterRejected));

        assertThat(underTest.findEnforcementsCurrentFirst(caseWithClaim()))
            .containsExactly(live, laterRejected, olderClosed, unknownTime);
    }

    @Test
    void shouldFindNothingWhenTheClaimHasNoEnforcement() {
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of());

        assertThat(underTest.findCurrentEnforcement(caseWithClaim())).isEmpty();
    }

    @Test
    void shouldFindNothingWhenTheCaseHasNoClaim() {
        PcsCaseEntity noClaims = new PcsCaseEntity();
        noClaims.setClaims(new ArrayList<>());

        assertThat(underTest.findCurrentEnforcement(noClaims)).isEmpty();
        assertThat(underTest.findCurrentEnforcement((PcsCaseEntity) null)).isEmpty();
        assertThat(underTest.findEnforcementsCurrentFirst(null)).isEmpty();
        verifyNoInteractions(enforcementOrderRepository);
    }

    @Test
    void shouldLoadTheCaseByReference() {
        EnforcementOrderEntity live = enforcement(EnforcementStatus.AWAITING_SCHEDULING, EARLIER);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseWithClaim());
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(live));

        assertThat(underTest.findCurrentEnforcement(CASE_REFERENCE)).contains(live);
    }

    @Test
    void shouldLoadTheEnforcementsOnceWhileTheyAreLoadedOnce() {
        EnforcementOrderEntity live = enforcement(EnforcementStatus.AWAITING_SCHEDULING, EARLIER);
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(live));
        PcsCaseEntity pcsCase = caseWithClaim();

        CurrentEnforcementService.withEnforcementsLoadedOnce(() -> {
            assertThat(underTest.findCurrentEnforcement(pcsCase)).contains(live);
            CurrentEnforcementService.withEnforcementsLoadedOnce(
                () -> assertThat(underTest.findEnforcementsCurrentFirst(pcsCase)).containsExactly(live));
            assertThat(underTest.findCurrentEnforcement(caseWithClaim())).contains(live);
        });

        verify(enforcementOrderRepository, times(1)).findByClaimId(CLAIM_ID);
    }

    @Test
    void shouldLoadTheEnforcementsAgainOutsideLoadedOnce() {
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = caseWithClaim();

        CurrentEnforcementService.withEnforcementsLoadedOnce(() -> underTest.findCurrentEnforcement(pcsCase));
        underTest.findCurrentEnforcement(pcsCase);
        underTest.findCurrentEnforcement(pcsCase);

        verify(enforcementOrderRepository, times(3)).findByClaimId(CLAIM_ID);
    }

    private static PcsCaseEntity caseWithClaim() {
        ClaimEntity claim = new ClaimEntity();
        claim.setId(CLAIM_ID);
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setClaims(new ArrayList<>(List.of(claim)));
        return pcsCase;
    }

    private static EnforcementOrderEntity enforcement(EnforcementStatus status, Instant statusUpdatedAt) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(statusUpdatedAt);
        return enforcement;
    }
}
