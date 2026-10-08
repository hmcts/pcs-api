package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PcsCaseEntityTest {

    private PcsCaseEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsCaseEntity();
    }

    @Test
    void shouldUpdateCaseOnTenancyLicenceWhenSet() {
        // Given
        TenancyLicenceEntity existingTenancyLicence = mock(TenancyLicenceEntity.class);
        TenancyLicenceEntity updatedTenancyLicence = mock(TenancyLicenceEntity.class);
        underTest.setTenancyLicence(existingTenancyLicence);

        // When
        underTest.setTenancyLicence(updatedTenancyLicence);

        // Then
        verify(existingTenancyLicence).setPcsCase(null);
        verify(updatedTenancyLicence).setPcsCase(underTest);
    }

    @Test
    void shouldAddGenAppEntityAndSetCaseLevelRank() {
        // Given
        GenAppEntity genAppEntity1 = mock(GenAppEntity.class);
        GenAppEntity genAppEntity2 = mock(GenAppEntity.class);
        GenAppEntity genAppEntity3 = mock(GenAppEntity.class);

        // When
        underTest.addGenApp(genAppEntity1);
        underTest.addGenApp(genAppEntity2);
        underTest.addGenApp(genAppEntity3);

        // Then
        verify(genAppEntity1).setRank(1);
        verify(genAppEntity1).setPcsCase(underTest);

        verify(genAppEntity2).setRank(2);
        verify(genAppEntity2).setPcsCase(underTest);

        verify(genAppEntity3).setRank(3);
        verify(genAppEntity3).setPcsCase(underTest);
    }

    @Test
    void shouldContinueGenAppCaseLevelRankFromExistingApplications() {
        // Given
        for (int i = 0; i < 5; i++) {
            underTest.getGenApps().add(mock(GenAppEntity.class));
        }
        GenAppEntity genAppEntity = mock(GenAppEntity.class);

        // When
        underTest.addGenApp(genAppEntity);

        // Then
        verify(genAppEntity).setRank(6);
        verify(genAppEntity).setPcsCase(underTest);
    }

    @Test
    void shouldNotAssignRankToPendingCounterClaim() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        underTest.addCounterClaim(counterClaim);

        assertThat(counterClaim.getRank()).isNull();
        assertThat(counterClaim.getPcsCase()).isSameAs(underTest);
    }

    @Test
    void shouldAssignRankWhenIssuedCounterClaimAdded() {
        CounterClaimEntity counterClaim1 = CounterClaimEntity.builder()
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();
        CounterClaimEntity counterClaim2 = CounterClaimEntity.builder()
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();

        underTest.addCounterClaim(counterClaim1);
        underTest.addCounterClaim(counterClaim2);

        assertThat(counterClaim1.getRank()).isEqualTo(1);
        assertThat(counterClaim1.getPcsCase()).isSameAs(underTest);

        assertThat(counterClaim2.getRank()).isEqualTo(2);
        assertThat(counterClaim2.getPcsCase()).isSameAs(underTest);
    }

    @Test
    void shouldAssignRankToCounterClaimWhenIssuedLater() {
        CounterClaimEntity pendingCounterClaim = CounterClaimEntity.builder()
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();
        underTest.addCounterClaim(pendingCounterClaim);
        assertThat(pendingCounterClaim.getRank()).isNull();

        pendingCounterClaim.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);
        underTest.assignCounterClaimRank(pendingCounterClaim);

        assertThat(pendingCounterClaim.getRank()).isEqualTo(1);
    }

    @Test
    void shouldAddHearing() {
        // Given
        HearingEntity hearingEntity = mock(HearingEntity.class);

        // When
        underTest.addHearing(hearingEntity);

        // Then
        assertThat(underTest.getHearings()).hasSize(1);
        assertThat(underTest.getHearings().getFirst()).isEqualTo(hearingEntity);
        verify(hearingEntity).setPcsCase(underTest);
    }

}
