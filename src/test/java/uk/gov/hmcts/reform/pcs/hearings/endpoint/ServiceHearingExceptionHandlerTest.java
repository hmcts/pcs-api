package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler.Error;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.FeatureNotEnabledException;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingRequest;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ServiceHearingExceptionHandlerTest {

    private static final String PATH = "/serviceHearingValues";

    private ServiceHearingExceptionHandler underTest;
    private MockHttpServletRequest request;
    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        underTest = new ServiceHearingExceptionHandler();
        request = new MockHttpServletRequest("POST", PATH);

        handlerLogger = (Logger) LoggerFactory.getLogger(ServiceHearingExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(logAppender);
    }

    @Test
    void shouldReturnBadRequestWithEveryValidationMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ServiceHearingRequest(), "request");
        bindingResult.addError(new FieldError("request", "caseId", "caseReference must be a 16-digit case reference"));
        bindingResult.addError(new FieldError("request", "caseId", "caseReference is required"));
        MethodArgumentNotValidException ex =
            new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<Error> response = underTest.handleValidationFailure(ex, request);

        String expected = "caseReference is required; caseReference must be a 16-digit case reference";
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new Error(expected));
        assertLoggedError("Bad request to " + PATH + ": " + expected);
    }

    @Test
    void shouldReturnBadRequestForUnreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
            "JSON parse error", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Error> response = underTest.handleUnreadableBody(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new Error(ServiceHearingExceptionHandler.REQUEST_BODY_INVALID));
        assertLoggedError("Bad request to " + PATH + ": " + ServiceHearingExceptionHandler.REQUEST_BODY_INVALID);
    }

    @Test
    void shouldReturnBadRequestForMissingHeader() {
        MissingRequestHeaderException ex =
            new MissingRequestHeaderException("ServiceAuthorization", mock(MethodParameter.class));

        ResponseEntity<Error> response = underTest.handleMissingHeader(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new Error("Missing required header: ServiceAuthorization"));
        assertLoggedError("Bad request to " + PATH + ": Missing required header: ServiceAuthorization");
    }

    @Test
    void shouldReturnNotFoundAndLogCaseReference() {
        ResponseEntity<Error> response =
            underTest.handleCaseNotFound(new CaseNotFoundException(1234567890123456L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new Error(ServiceHearingExceptionHandler.CASE_NOT_FOUND));
        assertLoggedError("Not found for " + PATH + ": No case found with reference 1234567890123456");
    }

    @Test
    void shouldReturnNotFoundWhenFeatureDisabled() {
        ResponseEntity<Error> response =
            underTest.handleFeatureNotEnabled(new FeatureNotEnabledException(FeatureFlag.HMC_INTEGRATION), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new Error(ServiceHearingExceptionHandler.HMC_INTEGRATION_DISABLED));
        assertLoggedError("Not available for " + PATH + ": Feature flag hmc-integration-enabled is not enabled");
    }

    @Test
    void shouldReturnInternalServerErrorWithoutLeakingDetails() {
        IllegalArgumentException cause = new IllegalArgumentException("internal detail");

        ResponseEntity<Error> response = underTest.handleUnexpected(cause, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new Error(ServiceHearingExceptionHandler.UNEXPECTED_ERROR));
        assertLoggedError("Unexpected error handling " + PATH);
        assertThat(logAppender.list.getFirst().getThrowableProxy().getMessage()).isEqualTo("internal detail");
    }

    private void assertLoggedError(String expectedMessage) {
        assertThat(logAppender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage()).isEqualTo(expectedMessage);
        });
    }
}
