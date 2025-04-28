package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerTest {

    private RestExceptionHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new RestExceptionHandler();
    }

    @Test
    void shouldHandleCaseNotFoundException() {
        // Given
        String expectedErrorMessage = "Some error message";

        CaseNotFoundException caseNotFoundException = mock(CaseNotFoundException.class);
        when(caseNotFoundException.getMessage()).thenReturn(expectedErrorMessage);

        // When
        ResponseEntity<RestExceptionHandler.Error> responseEntity
            = underTest.handleCaseNotFoundException(caseNotFoundException);

        // Then
        assertThat(responseEntity.getBody().message()).isEqualTo(expectedErrorMessage);
    }

}
