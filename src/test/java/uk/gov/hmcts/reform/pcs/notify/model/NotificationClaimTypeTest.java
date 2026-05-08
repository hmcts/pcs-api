package uk.gov.hmcts.reform.pcs.notify.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationClaimTypeTest {
    @ParameterizedTest
    @EnumSource(NotificationClaimType.class)
    void shouldReturnCorrectValue(NotificationClaimType claimType) {
        String expectedValue = switch (claimType) {
            case COUNTER_CLAIM -> "counter_claim";
            case POSSESSION_CLAIM -> "possession_claim";
            case GENERAL_APPLICATION_CLAIM -> "general_application_claim";
        };

        assertThat(claimType.getValue()).isEqualTo(expectedValue);
    }

    @Test
    void shouldReturnClaimTypeFromValidString() {
        assertThat(NotificationClaimType.fromString("counter_claim"))
            .isEqualTo(NotificationClaimType.COUNTER_CLAIM);
        assertThat(NotificationClaimType.fromString("possession_claim"))
            .isEqualTo(NotificationClaimType.POSSESSION_CLAIM);
        assertThat(NotificationClaimType.fromString("general_application_claim"))
            .isEqualTo(NotificationClaimType.GENERAL_APPLICATION_CLAIM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"COUNTER_CLAIM", "Possession_Claim", "GENERAL_APPLICATION_CLAIM"})
    void shouldReturnClaimTypeFromValidStringIgnoringCase(String value) {
        NotificationClaimType result = NotificationClaimType.fromString(value);
        assertThat(result.getValue()).isEqualToIgnoringCase(value);
    }

    @Test
    void shouldThrowExceptionForUnknownStatus() {
        assertThatThrownBy(() -> NotificationClaimType.fromString("unknown_claim"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown status: unknown_claim");
    }

    @Test
    void shouldThrowExceptionForNullStatus() {
        assertThatThrownBy(() -> NotificationClaimType.fromString(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown status: null");
    }
}
