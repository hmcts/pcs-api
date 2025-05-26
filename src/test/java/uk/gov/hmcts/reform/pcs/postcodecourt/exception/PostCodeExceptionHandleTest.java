package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostCodeExceptionHandleTest {

    private final PostCodeExceptionHandler handler = new PostCodeExceptionHandler();

    @Test
    void shouldReturnInternalServerErrorForNotificationException() {
        PostCodeNotFoundException exception = mock(PostCodeNotFoundException.class);

        when(exception.getMessage()).thenReturn("No court mapping found for postcode");
        ResponseEntity<PostCodeExceptionHandler.Error> response = handler.handlePostCodeNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}

