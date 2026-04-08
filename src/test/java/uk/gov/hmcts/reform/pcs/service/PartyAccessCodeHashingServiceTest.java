package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PartyAccessCodeHashingServiceTest {

    private PartyAccessCodeHashingService underTest;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        underTest = new PartyAccessCodeHashingService(encoder, true);
    }

    @Test
    void shouldHashAndMatch() {
        String accessCode = "ABCD1234";

        String hash = underTest.hash(accessCode);

        assertThat(hash).isNotEqualTo(accessCode);
        assertThat(underTest.matches(accessCode, hash)).isTrue();
        assertThat(underTest.matches("WRONG", hash)).isFalse();
    }

    @Test
    void shouldThrowWhenAccessCodeBlankOrNull() {
        assertThatThrownBy(() -> underTest.hash(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access Code cannot be null or empty");

        assertThatThrownBy(() -> underTest.hash(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access Code cannot be null or empty");

        assertThatThrownBy(() -> underTest.hash(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access Code cannot be null or empty");
    }

    @Test
    void shouldReturnPlainTextWhenHashingDisabled() {
        PartyAccessCodeHashingService disabledService =
            new PartyAccessCodeHashingService(encoder, false);

        String accessCode = "ABCD1234";
        String stored = disabledService.hash(accessCode);

        assertThat(stored).isEqualTo(accessCode); // plain text
        assertThat(disabledService.matches(accessCode, stored)).isTrue();
        assertThat(disabledService.matches("WRONG", stored)).isFalse();
    }

}
