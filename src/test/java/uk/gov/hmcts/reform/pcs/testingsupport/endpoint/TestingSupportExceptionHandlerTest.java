package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestingSupportExceptionHandlerTest {

    private final TestingSupportExceptionHandler handler = new TestingSupportExceptionHandler();

    @Test
    void shouldReturnInternalServerErrorForNotificationException() {
        NotificationException exception = mock(NotificationException.class);
        when(exception.getMessage()).thenReturn("Failed to send notification");
        
        ResponseEntity<RestExceptionHandler.Error> response = handler.handleNotificationException(exception);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        RestExceptionHandler.Error errorResponse = response.getBody();
        assertNotNull(errorResponse);
    }
}
