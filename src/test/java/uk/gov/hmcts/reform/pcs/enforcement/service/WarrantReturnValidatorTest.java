package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.InterimWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.SuccessfulWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.UnsuccessfulWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

class WarrantReturnValidatorTest {

    // "Given today is 08/07/2026 and the time is 10:00" (BST).
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 8, 10, 0);

    private final WarrantReturnValidator underTest = new WarrantReturnValidator(
        Clock.fixed(NOW.atZone(UK_ZONE_ID).toInstant(), UK_ZONE_ID));

    @Test
    void shouldRequireAReturn() {
        assertThat(underTest.validate(null, CLAIMANT_CONFIRMED)).containsExactly("Select a return");
        assertThat(underTest.validate(WarrantReturnDetails.builder().build(), CLAIMANT_CONFIRMED))
            .containsExactly("Select a return");
    }

    @Test
    void shouldRequireAnOutcomeForAFinalReturn() {
        WarrantReturnDetails details = WarrantReturnDetails.builder().type(WarrantReturnType.FINAL).build();

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED)).containsExactly("Select an outcome");
    }

    @ParameterizedTest
    @EnumSource(WarrantReturnOutcome.class)
    void shouldRequireAReturnNameForAFinalReturn(WarrantReturnOutcome outcome) {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(outcome)
            .evictionDateTime(NOW.minusHours(1))
            .build();

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED)).containsExactly("Select a return name");
    }

    @Test
    void shouldRequireAReturnNameForAnInterimReturn() {
        WarrantReturnDetails details = WarrantReturnDetails.builder().type(WarrantReturnType.INTERIM).build();

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED)).containsExactly("Select a return name");
    }

    @Test
    void shouldAcceptASuccessfulEvictionEarlierToday() {
        assertThat(underTest.validate(successfulEviction(NOW.minusMinutes(45)), CLAIMANT_CONFIRMED)).isEmpty();
    }

    @Test
    void shouldAcceptASuccessfulEvictionNowAndInThePast() {
        assertThat(underTest.validate(successfulEviction(NOW), CLAIMANT_CONFIRMED)).isEmpty();
        assertThat(underTest.validate(successfulEviction(NOW.minusYears(1)), CLAIMANT_CONFIRMED)).isEmpty();
    }

    // "Successful eviction needs the eviction date and time".
    @Test
    void shouldRejectASuccessfulEvictionLaterToday() {
        assertThat(underTest.validate(successfulEviction(LocalDateTime.of(2026, 7, 8, 16, 0)), CLAIMANT_CONFIRMED))
            .containsExactly("The eviction date and time cannot be in the future");
    }

    @Test
    void shouldRequireTheEvictionDateAndTimeForASuccessfulEviction() {
        assertThat(underTest.validate(successfulEviction(null), CLAIMANT_CONFIRMED))
            .containsExactly("Enter the date and time of the eviction");
    }

    @ParameterizedTest
    @EnumSource(value = WarrantReturnOutcome.class, names = {"EVICTION_UNSUCCESSFUL", "EVICTION_CANCELLED"})
    void shouldIgnoreTheEvictionDateAndTimeUnlessTheEvictionWasSuccessful(WarrantReturnOutcome outcome) {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(outcome)
            .unsuccessfulReturnName(UnsuccessfulWarrantReturnName.WARRANT_EXPIRED)
            .evictionDateTime(NOW.plusDays(1))
            .build();

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\n"})
    void shouldRequireAdditionalDetailsForAReturnNameThatNeedsThem(String additionalDetails) {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, additionalDetails);

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED)).containsExactly("Enter additional details");
    }

    @Test
    void shouldNotRequireAdditionalDetailsForAReturnNameThatDoesNotNeedThem() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.OBSTRUCTION_BY_DEFENDANTS, null);

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED)).isEmpty();
    }

    @Test
    void shouldAcceptAdditionalDetailsOfTheMaximumLength() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, "a".repeat(6800));

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED)).isEmpty();
    }

    @Test
    void shouldRejectAdditionalDetailsThatAreTooLong() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.OBSTRUCTION_BY_DEFENDANTS, "a".repeat(6801));

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED))
            .containsExactly("Additional details must be 6,800 characters or fewer");
    }

    @Test
    void shouldOnlyReopenAClosedWarrant() {
        WarrantReturnDetails reopen = interim(InterimWarrantReturnName.REOPEN_WARRANT, "Order discharged");

        assertThat(underTest.validate(reopen, CLOSED)).isEmpty();
        assertThat(underTest.validate(reopen, CLAIMANT_CONFIRMED))
            .containsExactly("Only a closed warrant can be reopened");
    }

    @ParameterizedTest
    @EnumSource(value = InterimWarrantReturnName.class, mode = EnumSource.Mode.EXCLUDE, names = "REOPEN_WARRANT")
    void shouldOnlyTakeReopenWarrantOnAClosedWarrant(InterimWarrantReturnName returnName) {
        assertThat(underTest.validate(interim(returnName, "details"), CLOSED))
            .containsExactly("The warrant is closed. The only return you can record is \"Reopen warrant\"");
    }

    @Test
    void shouldNotRecordASuccessfulEvictionOnAClosedWarrant() {
        assertThat(underTest.validate(successfulEviction(NOW), CLOSED))
            .containsExactly("The warrant is closed. The only return you can record is \"Reopen warrant\"");
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "CLOSED")
    void shouldRecordAnyOtherReturnOnALiveWarrant(EnforcementStatus status) {
        assertThat(underTest.validate(interim(InterimWarrantReturnName.CROWDS_OR_PROTESTS, null), status)).isEmpty();
    }

    @Test
    void shouldReportEveryProblemAtOnce() {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
            .evictionDateTime(NOW.plusMinutes(1))
            .build();

        assertThat(underTest.validate(details, CLAIMANT_CONFIRMED))
            .containsExactly("Select a return name", "The eviction date and time cannot be in the future");
    }

    @Test
    void shouldCompareTheEvictionTimeWithUkTime() {
        // 09:00 UTC is 10:00 BST, so an eviction entered as 09:30 (UK time, as XUI shows it) is in the past.
        WarrantReturnValidator utcNine = new WarrantReturnValidator(
            Clock.fixed(Instant.parse("2026-07-08T09:00:00Z"), UK_ZONE_ID));

        assertThat(utcNine.validate(successfulEviction(LocalDateTime.of(2026, 7, 8, 9, 30)), CLAIMANT_CONFIRMED))
            .isEmpty();
    }

    private static WarrantReturnDetails successfulEviction(LocalDateTime evictionDateTime) {
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
            .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
            .evictionDateTime(evictionDateTime)
            .build();
    }

    private static WarrantReturnDetails interim(InterimWarrantReturnName returnName, String additionalDetails) {
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .interimReturnName(returnName)
            .additionalDetails(additionalDetails)
            .build();
    }
}
