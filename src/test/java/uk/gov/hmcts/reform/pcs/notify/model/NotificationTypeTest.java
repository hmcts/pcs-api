package uk.gov.hmcts.reform.pcs.notify.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NotificationTypeTest {
    @Test
    void shouldReturnValueFromToString() {
        assertThat(NotificationType.EMAIL.toString()).isEqualTo("Email");
    }

    @Test
    void shouldConvertFromStringWhenExactMatch() {
        NotificationType type = NotificationType.fromString("Email");

        assertThat(type).isEqualTo(NotificationType.EMAIL);
    }

    @Test
    void shouldConvertFromStringWhenDifferentCase() {
        NotificationType typeLower = NotificationType.fromString("email");
        NotificationType typeUpper = NotificationType.fromString("EMAIL");

        assertThat(typeLower).isEqualTo(NotificationType.EMAIL);
        assertThat(typeUpper).isEqualTo(NotificationType.EMAIL);
    }

    @Test
    void shouldThrowExceptionWhenUnknownValue() {
        assertThatThrownBy(() -> NotificationType.fromString("AnotherType"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown status: AnotherType");
    }

    @Test
    void shouldThrowExceptionWhenNull() {
        assertThatThrownBy(() -> NotificationType.fromString(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
