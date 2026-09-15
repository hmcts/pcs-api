package uk.gov.hmcts.reform.pcs.security;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAuthTokenValidatorTest {

    @Mock
    private ServiceAuthorisationApi serviceAuthorisationApi;

    private ServiceAuthTokenValidator validator;

    private static final String TEST_TOKEN = "Bearer standard-mock-token";
    private static final String SERVICE_NAME = "pcs-service";

    @BeforeEach
    void setUp() {
        validator = new ServiceAuthTokenValidator(serviceAuthorisationApi);
    }
    
    @Test
    @DisplayName("Should successfully validate token with no roles specified")
    void validateTokenWithoutRolesSuccess() {
        // Act
        validator.validate(TEST_TOKEN);

        // Assert
        verify(serviceAuthorisationApi).authorise(eq(TEST_TOKEN), any(String[].class));
    }

    @Test
    @DisplayName("Should successfully validate token with explicit roles list")
    void validateTokenWithRolesSuccess() {
        List<String> roles = List.of("role-1", "role-2");

        // Act
        validator.validate(TEST_TOKEN, roles);

        // Assert
        verify(serviceAuthorisationApi).authorise(eq(TEST_TOKEN), eq(new String[]{"role-1", "role-2"}));
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when api returns 4xx Client Error during validation")
    void validateTokenThrowsInvalidTokenExceptionOnClientError() {
        // Arrange
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(401);
        when(feignException.getMessage()).thenReturn("Unauthorized client token");

        doThrow(feignException).when(serviceAuthorisationApi).authorise(eq(TEST_TOKEN), any(String[].class));

        // Act & Assert
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
            validator.validate(TEST_TOKEN, Collections.emptyList())
        );
        assertEquals("Unauthorized client token", exception.getMessage());
        assertEquals(feignException, exception.getCause());
    }

    @Test
    @DisplayName("Should throw ServiceException when api returns 5xx Server Error during validation")
    void validateTokenThrowsServiceExceptionOnServerError() {
        // Arrange
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal Auth Server Error");

        doThrow(feignException).when(serviceAuthorisationApi).authorise(eq(TEST_TOKEN), any(String[].class));

        // Act & Assert
        ServiceException exception = assertThrows(ServiceException.class, () ->
            validator.validate(TEST_TOKEN, Collections.emptyList())
        );
        assertEquals("Internal Auth Server Error", exception.getMessage());
        assertEquals(feignException, exception.getCause());
    }

    @Test
    @DisplayName("Should successfully return service name from valid token")
    void getServiceNameSuccess() {
        // Arrange
        when(serviceAuthorisationApi.getServiceName(TEST_TOKEN)).thenReturn(SERVICE_NAME);

        // Act
        String result = validator.getServiceName(TEST_TOKEN);

        // Assert
        assertEquals(SERVICE_NAME, result);
        verify(serviceAuthorisationApi).getServiceName(TEST_TOKEN);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when api returns 4xx Client Error getting service name")
    void getServiceNameThrowsInvalidTokenExceptionOnClientError() {
        // Arrange
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(403);
        when(feignException.getMessage()).thenReturn("Forbidden token scope");

        when(serviceAuthorisationApi.getServiceName(TEST_TOKEN)).thenThrow(feignException);

        // Act & Assert
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
            validator.getServiceName(TEST_TOKEN)
        );
        assertEquals("Forbidden token scope", exception.getMessage());
        assertEquals(feignException, exception.getCause());
    }

    @Test
    @DisplayName("Should throw ServiceException when api returns 5xx Server Error getting service name")
    void getServiceNameThrowsServiceExceptionOnServerError() {
        // Arrange
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(503);
        when(feignException.getMessage()).thenReturn("S2S Service Unavailable");

        when(serviceAuthorisationApi.getServiceName(TEST_TOKEN)).thenThrow(feignException);

        // Act & Assert
        ServiceException exception = assertThrows(ServiceException.class, () ->
            validator.getServiceName(TEST_TOKEN)
        );
        assertEquals("S2S Service Unavailable", exception.getMessage());
        assertEquals(feignException, exception.getCause());
    }
}
