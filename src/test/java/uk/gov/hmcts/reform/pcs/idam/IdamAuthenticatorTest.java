package uk.gov.hmcts.reform.pcs.idam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamAuthenticatorTest {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String RAW_TOKEN = "valid-token";
    private static final String BEARER_TOKEN = BEARER_PREFIX + RAW_TOKEN;

    @Mock
    private JwtDecoder idamJwtDecoder;

    @InjectMocks
    private IdamAuthenticator underTest;

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should throw InvalidAuthTokenException when token is null or blank")
    void shouldThrowInvalidAuthTokenExceptionWhenAuthTokenIsNullOrBlank(String authToken) {
        assertThatThrownBy(() -> underTest.validateAuthToken(authToken))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessage("Authorization token is null or blank");

        verifyNoInteractions(idamJwtDecoder);
    }

    @Test
    @DisplayName("Should throw InvalidAuthTokenException when token is malformed")
    void shouldThrowInvalidAuthTokenExceptionWhenAuthTokenMalformed() {
        assertThatThrownBy(() -> underTest.validateAuthToken("InvalidToken"))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessageContaining("Malformed Authorization token");

        verifyNoInteractions(idamJwtDecoder);
    }

    @Test
    @DisplayName("Should return user with claims mapped from decoded JWT")
    void shouldReturnUserWhenTokenIsValid() {
        Jwt jwt = jwtWithClaims(Map.of(
            "sub", "user@test.com",
            "uid", "abc-123",
            "name", "Alice Smith",
            "given_name", "Alice",
            "family_name", "Smith",
            "roles", List.of("caseworker", "caseworker-pcs")
        ));
        when(idamJwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);

        User user = underTest.validateAuthToken(BEARER_TOKEN);

        assertThat(user).isNotNull();
        assertThat(user.getAuthToken()).isEqualTo(BEARER_TOKEN);
        assertThat(user.getUserDetails().getUid()).isEqualTo("abc-123");
        assertThat(user.getUserDetails().getSub()).isEqualTo("user@test.com");
        assertThat(user.getUserDetails().getName()).isEqualTo("Alice Smith");
        assertThat(user.getUserDetails().getGivenName()).isEqualTo("Alice");
        assertThat(user.getUserDetails().getFamilyName()).isEqualTo("Smith");
        assertThat(user.getUserDetails().getRoles()).containsExactly("caseworker", "caseworker-pcs");
        verify(idamJwtDecoder).decode(RAW_TOKEN);
    }

    @Test
    @DisplayName("Should throw InvalidAuthTokenException when JWT decode fails")
    void shouldThrowInvalidAuthTokenExceptionWhenJwtDecodeFails() {
        BadJwtException badJwt = new BadJwtException("expired or signature mismatch");
        when(idamJwtDecoder.decode(RAW_TOKEN)).thenThrow(badJwt);

        assertThatThrownBy(() -> underTest.validateAuthToken(BEARER_TOKEN))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessage("The Authorization token provided is expired or invalid")
            .hasCause(badJwt);
    }

    @Test
    @DisplayName("retrieveUser strips Bearer prefix before decoding")
    void retrieveUserStripsBearerPrefixBeforeDecoding() {
        Jwt jwt = jwtWithClaims(Map.of("uid", "abc-123"));
        when(idamJwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);

        User user = underTest.retrieveUser(BEARER_TOKEN);

        assertThat(user.getAuthToken()).isEqualTo(BEARER_TOKEN);
        assertThat(user.getUserDetails().getUid()).isEqualTo("abc-123");
        verify(idamJwtDecoder).decode(RAW_TOKEN);
    }

    @Test
    @DisplayName("retrieveUser handles raw token without Bearer prefix")
    void retrieveUserHandlesRawTokenWithoutBearerPrefix() {
        Jwt jwt = jwtWithClaims(Map.of("uid", "abc-123"));
        when(idamJwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);

        User user = underTest.retrieveUser(RAW_TOKEN);

        assertThat(user.getAuthToken()).isEqualTo(BEARER_TOKEN);
    }

    private Jwt jwtWithClaims(Map<String, Object> claims) {
        return Jwt.withTokenValue("token-value")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claims(c -> c.putAll(claims))
            .build();
    }
}
