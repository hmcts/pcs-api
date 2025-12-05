package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeRequest;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeResponse;
import uk.gov.hmcts.reform.pcs.service.CasePartyLinkService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasePartyLinkControllerTest {

    private static final long CASE_REFERENCE = 12345L;
    private static final String ACCESS_CODE = "ABC123XYZ789";
    private static final String AUTH_HEADER = "Bearer test-token";
    private static final String SERVICE_AUTH_HEADER = "service-auth-token";

    @Mock
    private IdamService idamService;

    @Mock
    private CasePartyLinkService casePartyLinkService;

    @InjectMocks
    private CasePartyLinkController casePartyLinkController;

    @Test
    void shouldSuccessfullyValidateAndLinkParty() {
        // Given
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);
        UserInfo userInfo = mock(UserInfo.class);
        User user = mock(User.class);
        ValidateAccessCodeResponse expectedResponse = new ValidateAccessCodeResponse(CASE_REFERENCE, "linked");

        when(idamService.validateAuthToken(AUTH_HEADER)).thenReturn(user);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(casePartyLinkService.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<ValidateAccessCodeResponse> response = casePartyLinkController.validateAccessCode(
                CASE_REFERENCE, request, AUTH_HEADER, SERVICE_AUTH_HEADER);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(response.getBody().getStatus()).isEqualTo("linked");

        verify(idamService).validateAuthToken(AUTH_HEADER);
        verify(casePartyLinkService).validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo);
    }

    @Test
    void shouldPropagateInvalidAuthTokenException() {
        // Given
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);
        when(idamService.validateAuthToken(AUTH_HEADER))
                .thenThrow(new InvalidAuthTokenException("Invalid token"));

        // When/Then
        assertThatThrownBy(() -> casePartyLinkController.validateAccessCode(
                CASE_REFERENCE, request, AUTH_HEADER, SERVICE_AUTH_HEADER))
                .isInstanceOf(InvalidAuthTokenException.class)
                .hasMessage("Invalid token");

        verify(idamService).validateAuthToken(AUTH_HEADER);
        verify(casePartyLinkService, never())
                .validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, null);
    }

    @Test
    void shouldPropagateCaseNotFoundExceptionFromService() {
        // Given
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);
        UserInfo userInfo = mock(UserInfo.class);
        User user = mock(User.class);

        when(idamService.validateAuthToken(AUTH_HEADER)).thenReturn(user);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(casePartyLinkService.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo))
                .thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        // When/Then
        assertThatThrownBy(() -> casePartyLinkController.validateAccessCode(
                CASE_REFERENCE, request, AUTH_HEADER, SERVICE_AUTH_HEADER))
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage("No case found with reference " + CASE_REFERENCE);

        verify(idamService).validateAuthToken(AUTH_HEADER);
        verify(casePartyLinkService).validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo);
    }

    @Test
    void shouldPropagateIllegalStateExceptionFromService() {
        // Given
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);
        UserInfo userInfo = mock(UserInfo.class);
        User user = mock(User.class);

        when(idamService.validateAuthToken(AUTH_HEADER)).thenReturn(user);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(casePartyLinkService.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo))
                .thenThrow(new IllegalStateException("User already linked"));

        // When/Then
        assertThatThrownBy(() -> casePartyLinkController.validateAccessCode(
                CASE_REFERENCE, request, AUTH_HEADER, SERVICE_AUTH_HEADER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User already linked");

        verify(idamService).validateAuthToken(AUTH_HEADER);
        verify(casePartyLinkService).validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo);
    }

}
