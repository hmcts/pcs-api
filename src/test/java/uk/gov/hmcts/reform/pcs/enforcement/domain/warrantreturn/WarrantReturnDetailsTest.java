package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class WarrantReturnDetailsTest {

    @Test
    void shouldSelectNothingWithoutAReturn() {
        assertThat(WarrantReturnDetails.selectedReturnName(null)).isEmpty();
        assertThat(WarrantReturnDetails.selectedReturnName(WarrantReturnDetails.builder().build())).isEmpty();
    }

    @Test
    void shouldSelectTheInterimReturnNameForAnInterimReturn() {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
            .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
            .interimReturnName(InterimWarrantReturnName.CROWDS_OR_PROTESTS)
            .build();

        assertThat(WarrantReturnDetails.selectedReturnName(details)).contains(WarrantReturnName.CROWDS_OR_PROTESTS);
    }

    @Test
    void shouldSelectNothingForAFinalReturnWithoutAnOutcome() {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
            .build();

        assertThat(WarrantReturnDetails.selectedReturnName(details)).isEmpty();
    }

    @Test
    void shouldSelectTheSuccessfulReturnNameForASuccessfulEviction() {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
            .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
            .unsuccessfulReturnName(UnsuccessfulWarrantReturnName.WARRANT_EXPIRED)
            .build();

        assertThat(WarrantReturnDetails.selectedReturnName(details)).contains(WarrantReturnName.EVICTION_SUCCESSFUL);
    }

    @ParameterizedTest
    @EnumSource(value = WarrantReturnOutcome.class, names = {"EVICTION_UNSUCCESSFUL", "EVICTION_CANCELLED"})
    void shouldSelectTheUnsuccessfulReturnNameOtherwise(WarrantReturnOutcome outcome) {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(outcome)
            .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
            .unsuccessfulReturnName(UnsuccessfulWarrantReturnName.WARRANT_EXPIRED)
            .build();

        assertThat(WarrantReturnDetails.selectedReturnName(details)).contains(WarrantReturnName.WARRANT_EXPIRED);
    }

    @Test
    void shouldSelectNothingWhenTheShownDropDownIsEmpty() {
        WarrantReturnDetails details = WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_CANCELLED)
            .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
            .build();

        assertThat(WarrantReturnDetails.selectedReturnName(details)).isEmpty();
    }
}
