package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantReturnRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName.EVICTION_SUCCESSFUL;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName.REOPEN_WARRANT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER;

// 15-repeat-enforcement.feature "Fee for a further warrant" and 01 "Repeat warrant within 12 months of a
// suspended warrant is free" (HDPI-8424, OPEN-QUESTIONS 6 and 57).
@ExtendWith(MockitoExtension.class)
class RepeatWarrantFeeWaiverTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 8);
    private static final Instant NOW = TODAY.atTime(9, 0).atZone(UK_ZONE_ID).toInstant();
    private static final UUID CLAIM_ID = UUID.randomUUID();

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private WarrantReturnRepository warrantReturnRepository;

    private RepeatWarrantFeeWaiver underTest;

    @BeforeEach
    void setUp() {
        underTest = new RepeatWarrantFeeWaiver(enforcementOrderRepository, warrantReturnRepository,
                                               Clock.fixed(NOW, UK_ZONE_ID));
    }

    @ParameterizedTest(name = "{0}, generated {1} months ago: free {2}")
    @CsvSource({
        "WARRANT_SUSPENDED_BY_ORDER, 6, true",
        "WARRANT_SUSPENDED_BY_ORDER, 12, true",
        "WARRANT_SUSPENDED_BY_ORDER, 13, false",
        "EVICTION_SUCCESSFUL, 6, false",
        "POSSESSION_ORDER_SET_ASIDE, 6, false",
        "WARRANT_WITHDRAWN, 1, false",
        "NO_LOCKSMITH_CLAIMANT_FAILED_TO_ARRANGE, 1, false"
    })
    void shouldWaiveTheFeeOnlyAfterASuspensionWithin12MonthsOfGeneration(WarrantReturnName returnName,
                                                                         int monthsAgo, boolean free) {
        EnforcementOrderEntity warrant = warrant(CLOSED, TODAY.minusMonths(monthsAgo));
        givenEnforcements(warrant);
        givenReturns(finalReturn(warrant, returnName, NOW.minusSeconds(60)));

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isEqualTo(free);
    }

    @Test
    void shouldMeasureFromTheFirstWarrantNotTheSuspendedOne() {
        // The first warrant was generated 13 months ago; the suspended one, itself a free repeat, 2 months ago.
        EnforcementOrderEntity first = warrant(CLOSED, TODAY.minusMonths(13));
        EnforcementOrderEntity suspended = warrant(CLOSED, TODAY.minusMonths(2));
        givenEnforcements(suspended, first);
        givenReturns(finalReturn(suspended, WARRANT_SUSPENDED_BY_ORDER, NOW.minusSeconds(60)),
                     finalReturn(first, WARRANT_SUSPENDED_BY_ORDER, NOW.minusSeconds(86_400 * 90L)));

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isFalse();
    }

    @Test
    void shouldMeasureFromGenerationNotFromWhenTheReturnWasRecorded() {
        EnforcementOrderEntity warrant = warrant(CLOSED, TODAY.minusMonths(13));
        givenEnforcements(warrant);
        givenReturns(finalReturn(warrant, WARRANT_SUSPENDED_BY_ORDER, NOW.minusSeconds(60)));

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isFalse();
    }

    @Test
    void shouldIgnoreWritsAndRejectedApplicationsWhenFindingTheFirstWarrant() {
        EnforcementOrderEntity suspended = warrant(CLOSED, TODAY.minusMonths(6));
        EnforcementOrderEntity olderWrit = enforcement(SelectEnforcementType.WRIT, TRANSFERRED_TO_HIGH_COURT);
        olderWrit.setApprovedAt(generatedOn(TODAY.minusMonths(20)));
        EnforcementOrderEntity rejected = enforcement(SelectEnforcementType.WARRANT, REJECTED);
        givenEnforcements(suspended, olderWrit, rejected);
        givenReturns(finalReturn(suspended, WARRANT_SUSPENDED_BY_ORDER, NOW.minusSeconds(60)));

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isTrue();
    }

    @Test
    void shouldUseTheClaimsMostRecentFinalReturn() {
        EnforcementOrderEntity first = warrant(CLOSED, TODAY.minusMonths(8));
        EnforcementOrderEntity second = warrant(CLOSED, TODAY.minusMonths(4));
        givenEnforcements(first, second);
        givenReturns(finalReturn(second, EVICTION_SUCCESSFUL, NOW.minusSeconds(60)),
                     finalReturn(first, WARRANT_SUSPENDED_BY_ORDER, NOW.minusSeconds(86_400 * 30L)));

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isFalse();
    }

    @Test
    void shouldChargeWhenTheSuspendedWarrantWasReopened() {
        EnforcementOrderEntity reopened = warrant(AWAITING_BAILIFF_ALLOCATION, TODAY.minusMonths(3));
        givenReturns(
            WarrantReturnEntity.builder().enforcementOrder(reopened).returnType(WarrantReturnType.INTERIM)
                .returnName(REOPEN_WARRANT).recordedAt(NOW.minusSeconds(60)).build(),
            finalReturn(reopened, WARRANT_SUSPENDED_BY_ORDER, NOW.minusSeconds(120)));

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isFalse();
    }

    @Test
    void shouldChargeWhenNoWarrantHasAGenerationDate() {
        EnforcementOrderEntity warrant = enforcement(SelectEnforcementType.WARRANT, CLOSED);
        givenEnforcements(warrant);
        givenReturns(finalReturn(warrant, WARRANT_SUSPENDED_BY_ORDER, NOW.minusSeconds(60)));

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isFalse();
    }

    @Test
    void shouldChargeWithoutAnyReturns() {
        givenReturns();

        assertThat(underTest.isFeeWaived(CLAIM_ID, SelectEnforcementType.WARRANT)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = "WARRANT", mode = EnumSource.Mode.EXCLUDE)
    void shouldOnlyWaiveTheFeeForAWarrantOfPossession(SelectEnforcementType enforcementType) {
        assertThat(underTest.isFeeWaived(CLAIM_ID, enforcementType)).isFalse();
        verifyNoInteractions(warrantReturnRepository, enforcementOrderRepository);
    }

    @Test
    void shouldChargeWithoutAClaim() {
        assertThat(underTest.isFeeWaived(null, SelectEnforcementType.WARRANT)).isFalse();
        verifyNoInteractions(warrantReturnRepository, enforcementOrderRepository);
    }

    @ParameterizedTest(name = "generated {0}, applied {1}: free {2}")
    @CsvSource({
        "2025-07-08T10:00, 2026-07-08, true",
        "2025-07-08T10:00, 2026-07-09, false",
        // British Summer Time: 23:30 UTC on 7 July is 00:30 on 8 July in the UK.
        "2025-07-07T23:30, 2026-07-08, true",
        "2025-08-31T10:00, 2026-08-31, true",
        "2025-08-31T10:00, 2026-09-01, false",
        "2024-02-29T10:00, 2025-02-28, true",
        "2024-02-29T10:00, 2025-03-01, false"
    })
    void shouldCountCalendarMonthsInUkDates(String generatedUtc, LocalDate applied, boolean free) {
        Instant generated = Instant.parse(generatedUtc + ":00Z");

        assertThat(RepeatWarrantFeeWaiver.isWithinFreePeriod(generated, applied)).isEqualTo(free);
    }

    private void givenEnforcements(EnforcementOrderEntity... enforcements) {
        // Not read when the last final return already decides the answer.
        lenient().when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(enforcements));
    }

    private void givenReturns(WarrantReturnEntity... returns) {
        when(warrantReturnRepository.findByEnforcementOrderClaimIdOrderByRecordedAtDesc(CLAIM_ID))
            .thenReturn(List.of(returns));
    }

    private static EnforcementOrderEntity warrant(EnforcementStatus status, LocalDate generatedOn) {
        EnforcementOrderEntity warrant = enforcement(SelectEnforcementType.WARRANT, status);
        warrant.setApprovedAt(generatedOn(generatedOn));
        return warrant;
    }

    private static Instant generatedOn(LocalDate date) {
        return date.atTime(10, 0).atZone(UK_ZONE_ID).toInstant();
    }

    private static EnforcementOrderEntity enforcement(SelectEnforcementType type, EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        enforcement.setStatus(status);
        enforcement.setEnforcementOrder(EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                .value(DynamicStringListElement.builder().code(type.name()).label(type.getLabel()).build())
                .build())
            .build());
        return enforcement;
    }

    private static WarrantReturnEntity finalReturn(EnforcementOrderEntity warrant, WarrantReturnName name,
                                                   Instant recordedAt) {
        return WarrantReturnEntity.builder()
            .enforcementOrder(warrant)
            .returnType(WarrantReturnType.FINAL)
            .returnName(name)
            .recordedAt(recordedAt)
            .build();
    }
}
