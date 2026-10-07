package uk.gov.hmcts.reform.pcs.feesandpay.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PayHubCardPaymentStatusTest {

    @ParameterizedTest
    @CsvSource({
        "Success, true",
        "success, true",
        "Paid, true",
        "paid, true",
        "Failed, false",
        "Declined, false",
        "Pending, false",
        "Initiated, false",
        "'', false"
    })
    void shouldIdentifySuccessfulPayHubStatuses(String status, boolean expected) {
        assertThat(PayHubCardPaymentStatus.isSuccessful(status)).isEqualTo(expected);
    }
}
