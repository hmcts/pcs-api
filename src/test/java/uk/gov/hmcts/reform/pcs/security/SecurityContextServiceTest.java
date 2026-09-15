package uk.gov.hmcts.reform.pcs.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityContextServiceTest {

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private User user;

    private MockedStatic<SecurityContextHolder> securityContextHolder;

    private static final String SYSTEM_USER_ID = "78acf0a0-079b-3112-8cad-549c81b83510";

    private SecurityContextService underTest;

    @BeforeEach
    void setUp() {
        securityContextHolder = mockStatic(SecurityContextHolder.class);
        securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        underTest = new SecurityContextService(SYSTEM_USER_ID);
    }

    @AfterEach
    void tearDown() {
        securityContextHolder.close();
    }

    @Test
    @DisplayName("Should report the system user when the principal carries the configured system uid")
    void isSystemUserForConfiguredUid() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        UserInfo userDetails = mock(UserInfo.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(SYSTEM_USER_ID);

        assertThat(underTest.isSystemUser()).isTrue();
    }

    @Test
    @DisplayName("Should not report the system user for a real user's uid")
    void isSystemUserForOtherUid() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        UserInfo userDetails = mock(UserInfo.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(UUID.randomUUID().toString());

        assertThat(underTest.isSystemUser()).isFalse();
    }

    @Test
    @DisplayName("Should not report the system user when there is no authentication")
    void isSystemUserWithNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThat(underTest.isSystemUser()).isFalse();
    }

    @Test
    @DisplayName("Should get the user details from the security context")
    void getUserDetails() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        UserInfo expectedUserDetails = mock(UserInfo.class);
        when(user.getUserDetails()).thenReturn(expectedUserDetails);

        UserInfo actualUserDetails = underTest.getCurrentUserDetails();

        assertThat(actualUserDetails).isEqualTo(expectedUserDetails);
    }

    @Test
    @DisplayName("Should get the user auth token from the security context")
    void getCurrentUserAuthToken() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        String expectedAuthHeader = "Bearer user-token";
        when(user.getAuthToken()).thenReturn(expectedAuthHeader);

        String actualAuthToken = underTest.getCurrentUserAuthToken();

        assertThat(actualAuthToken).isEqualTo(expectedAuthHeader);
    }

    @Test
    @DisplayName("Should return null user details when no authentication in the security context")
    void getUserDetailsWhenNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        Exception exception = catchException(() -> underTest.getCurrentUserDetails());

        assertThat(exception)
            .isInstanceOf(SecurityContextException.class)
            .hasMessage("No authentication instance found");
    }

    @Test
    @DisplayName("Should return null user details when no principal in the security context")
    void getUserDetailsWhenNoPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        Exception exception = catchException(() -> underTest.getCurrentUserDetails());

        assertThat(exception)
            .isInstanceOf(SecurityContextException.class)
            .hasMessage("Authentication principal is null or not of the expected type");
    }

    @Test
    @DisplayName("Should return null user details for principal of wrong type in the security context")
    void getUserDetailsWhenPrincipalNotAUserType() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new Object());

        Exception exception = catchException(() -> underTest.getCurrentUserDetails());

        assertThat(exception)
            .isInstanceOf(SecurityContextException.class)
            .hasMessage("Authentication principal is null or not of the expected type");
    }

    @Test
    @DisplayName("Should get the user ID from the security context")
    void getUserId() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        UserInfo userDetails = mock(UserInfo.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        UUID expectedUserId = UUID.randomUUID();
        when(userDetails.getUid()).thenReturn(expectedUserId.toString());

        UUID actualUserId = underTest.getCurrentUserId();

        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("Should return null user ID when no authentication in the security context")
    void getUserIdWhenNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        Exception exception = catchException(() -> underTest.getCurrentUserId());

        assertThat(exception)
            .isInstanceOf(SecurityContextException.class)
            .hasMessage("No authentication instance found");
    }

    @Test
    @DisplayName("Should return null user ID when no principal in the security context")
    void getUserIdWhenNoPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        Exception exception = catchException(() -> underTest.getCurrentUserId());

        assertThat(exception)
            .isInstanceOf(SecurityContextException.class)
            .hasMessage("Authentication principal is null or not of the expected type");
    }

    @Test
    @DisplayName("Should return null user ID for principal of wrong type in the security context")
    void getUserIdWhenPrincipalNotAUserType() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new Object());

        Exception exception = catchException(() -> underTest.getCurrentUserId());

        assertThat(exception)
            .isInstanceOf(SecurityContextException.class)
            .hasMessage("Authentication principal is null or not of the expected type");
    }

}
