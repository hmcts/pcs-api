package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeRequest;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeLinkService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasePartyLinkControllerTest {

    private static final long CASE_REFERENCE = 123456789012L;
    private static final String ACCESS_CODE = "ABC123456789";
    private static final String AUTH_HEADER = "Bearer user-token";
    private static final String S2S_TOKEN = "service-token";

    @Mock
    private IdamAuthenticator idamAuthenticator;
    @Mock
    private PartyAccessCodeLinkService partyAccessCodeLinkService;

    @InjectMocks
    private CasePartyLinkController underTest;

    @Test
    void shouldValidateAccessCodeAndLinkParty() {
        UserInfo userDetails = mock(UserInfo.class);
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(idamAuthenticator.validateAuthToken(AUTH_HEADER)).thenReturn(user);

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);

        ResponseEntity<Void> response =
            underTest.validateAccessCode(CASE_REFERENCE, request, AUTH_HEADER, S2S_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(idamAuthenticator).validateAuthToken(AUTH_HEADER);
        // Must pass the UserInfo (not the wrapping User) — that's the controller's responsibility.
        verify(partyAccessCodeLinkService).linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, userDetails);
    }

    @Test
    void shouldPropagateInvalidAuthTokenExceptionAndNotCallLinkService() {
        when(idamAuthenticator.validateAuthToken(AUTH_HEADER))
            .thenThrow(new InvalidAuthTokenException("Malformed Authorization token"));

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);

        assertThatThrownBy(() ->
            underTest.validateAccessCode(CASE_REFERENCE, request, AUTH_HEADER, S2S_TOKEN))
            .isInstanceOf(InvalidAuthTokenException.class)
            .hasMessageContaining("Malformed Authorization token");

        verifyNoInteractions(partyAccessCodeLinkService);
    }

    @Test
    void shouldPropagateInvalidAccessCodeExceptionFromLinkService() {
        UserInfo userDetails = mock(UserInfo.class);
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(idamAuthenticator.validateAuthToken(AUTH_HEADER)).thenReturn(user);

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);

        InvalidAccessCodeException expected = new InvalidAccessCodeException("Invalid access code");
        // Mockito.doThrow on void: use the explicit syntax via the linkPartyByAccessCode signature.
        doThrow(expected)
            .when(partyAccessCodeLinkService)
            .linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, userDetails);

        assertThatThrownBy(() ->
            underTest.validateAccessCode(CASE_REFERENCE, request, AUTH_HEADER, S2S_TOKEN))
            .isInstanceOf(InvalidAccessCodeException.class)
            .hasMessageContaining("Invalid access code");
    }

    @Test
    void shouldPropagateCaseNotFoundExceptionFromLinkService() {
        UserInfo userDetails = mock(UserInfo.class);
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(idamAuthenticator.validateAuthToken(AUTH_HEADER)).thenReturn(user);

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);

        CaseNotFoundException expected = new CaseNotFoundException(CASE_REFERENCE);
        doThrow(expected)
            .when(partyAccessCodeLinkService)
            .linkPartyByAccessCode(CASE_REFERENCE, ACCESS_CODE, userDetails);

        assertThatThrownBy(() ->
            underTest.validateAccessCode(CASE_REFERENCE, request, AUTH_HEADER, S2S_TOKEN))
            .isInstanceOf(CaseNotFoundException.class);
    }

}
