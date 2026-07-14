package uk.gov.hmcts.reform.pcs.notify.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationClaimTypeTest {
    @ParameterizedTest
    @EnumSource(NotificationClaimType.class)
    void shouldReturnCorrectValue(NotificationClaimType claimType) {
        String expectedValue = switch (claimType) {
            case COUNTER_CLAIM -> "counter_claim";
            case NO_COUNTER_CLAIM -> "no_counter_claim";
            case POSSESSION_CLAIM -> "possession_claim";
            case GENERAL_APPLICATION -> "general_application";
        };

        assertThat(claimType.getValue()).isEqualTo(expectedValue);
    }

}
