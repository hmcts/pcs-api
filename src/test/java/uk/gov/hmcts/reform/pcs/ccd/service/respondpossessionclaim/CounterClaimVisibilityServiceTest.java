package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;

@ExtendWith(MockitoExtension.class)
class CounterClaimVisibilityServiceTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    private CounterClaimVisibilityService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimVisibilityService();
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = CounterClaimState.class, mode = EnumSource.Mode.EXCLUDE, names = "COUNTER_CLAIM_ISSUED")
    void shouldHideCounterClaimsThatHaveNotBeenPaidFor(CounterClaimState state) {
        CounterClaimEntity counterClaim = stubCounterClaim(state, CURRENT_USER_ID, null);

        assertThat(underTest.isCounterClaimVisibleToUser(counterClaim, CURRENT_USER_ID)).isFalse();
    }

    @Test
    void shouldShowIssuedCounterClaimToTheDefendantWhoRaisedIt() {
        CounterClaimEntity counterClaim = stubCounterClaim(COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, null);

        assertThat(underTest.isCounterClaimVisibleToUser(counterClaim, CURRENT_USER_ID)).isTrue();
    }

    @Test
    void shouldHideIssuedCounterClaimRaisedByAnotherDefendant() {
        CounterClaimEntity counterClaim = stubCounterClaim(COUNTER_CLAIM_ISSUED, UUID.randomUUID(), null);

        assertThat(underTest.isCounterClaimVisibleToUser(counterClaim, CURRENT_USER_ID)).isFalse();
    }

    @Test
    void shouldHideCounterClaimWithNoParty() {
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.getStatus()).thenReturn(COUNTER_CLAIM_ISSUED);
        when(counterClaim.getParty()).thenReturn(null);

        assertThat(underTest.isCounterClaimVisibleToUser(counterClaim, CURRENT_USER_ID)).isFalse();
    }

    @Test
    void shouldReturnEmptyForNullCounterClaim() {
        assertThat(underTest.isCounterClaimVisibleToUser(null, CURRENT_USER_ID)).isFalse();
    }

    @Test
    void shouldReturnEmptyForNullUser() {
        CounterClaimEntity counterClaim = stubCounterClaim(COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, null);

        assertThat(underTest.isCounterClaimVisibleToUser(counterClaim, null)).isFalse();
    }

    @Test
    void shouldReturnEmptyWhenCaseHasNoCounterClaims() {
        assertThat(underTest.getVisibleCounterClaimForUser(null, CURRENT_USER_ID)).isEmpty();
        assertThat(underTest.getVisibleCounterClaimForUser(Collections.emptyList(), CURRENT_USER_ID)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoCounterClaimIsVisible() {
        CounterClaimEntity unpaid = stubCounterClaim(
            CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, LocalDateTime.now());
        CounterClaimEntity otherParty = stubCounterClaim(
            COUNTER_CLAIM_ISSUED, UUID.randomUUID(), LocalDateTime.now());

        assertThat(underTest.getVisibleCounterClaimForUser(List.of(unpaid, otherParty), CURRENT_USER_ID)).isEmpty();
    }

    @Test
    void shouldReturnTheMostRecentlySubmittedVisibleCounterClaim() {
        LocalDateTime now = LocalDateTime.now();
        CounterClaimEntity older = stubCounterClaim(COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, now.minusDays(10));
        CounterClaimEntity newest = stubCounterClaim(COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, now);

        Optional<CounterClaimEntity> visible =
            underTest.getVisibleCounterClaimForUser(List.of(older, newest), CURRENT_USER_ID);

        assertThat(visible).contains(newest);
    }

    @Test
    void shouldIgnoreNullEntriesInTheCollection() {
        CounterClaimEntity visible = stubCounterClaim(COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, LocalDateTime.now());
        List<CounterClaimEntity> counterClaims = new ArrayList<>();
        counterClaims.add(null);
        counterClaims.add(visible);

        assertThat(underTest.getVisibleCounterClaimForUser(counterClaims, CURRENT_USER_ID)).contains(visible);
    }

    @Test
    void shouldPreferADatedCounterClaimOverOneWithNoSubmittedDate() {
        CounterClaimEntity undated = stubCounterClaim(COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, null);
        CounterClaimEntity dated = stubCounterClaim(COUNTER_CLAIM_ISSUED, CURRENT_USER_ID, LocalDateTime.now());

        assertThat(underTest.getVisibleCounterClaimForUser(List.of(undated, dated), CURRENT_USER_ID))
            .contains(dated);
    }

    private CounterClaimEntity stubCounterClaim(CounterClaimState state, UUID raisedByIdamId,
                                                LocalDateTime submittedDate) {
        PartyEntity party = mock(PartyEntity.class);
        lenient().when(party.getIdamId()).thenReturn(raisedByIdamId);

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        lenient().when(counterClaim.getStatus()).thenReturn(state);
        lenient().when(counterClaim.getParty()).thenReturn(party);
        lenient().when(counterClaim.getClaimSubmittedDate()).thenReturn(submittedDate);
        return counterClaim;
    }
}
