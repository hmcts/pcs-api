package uk.gov.hmcts.reform.pcs.enforcement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.runtime.CallbackController;

import java.util.List;

/**
 * Covers start callbacks only. A decentralised submit runs through the SDK's package-private
 * {@code ServicePersistenceController}, so a submit handler must return errors rather than throw.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = CallbackController.class)
public class EnforcementCallbackExceptionHandler {

    @ExceptionHandler(LiveEnforcementExistsException.class)
    public ResponseEntity<AboutToStartOrSubmitResponse<Object, Object>> handleLiveEnforcementExists(
        LiveEnforcementExistsException ex) {
        log.info("Enforcement event refused: {}", ex.getMessage());
        return ResponseEntity.ok(AboutToStartOrSubmitResponse.builder()
                                     .errors(List.of(ex.getMessage()))
                                     .build());
    }

    @ExceptionHandler(NoEnforcementAwaitingReviewException.class)
    public ResponseEntity<AboutToStartOrSubmitResponse<Object, Object>> handleNoEnforcementAwaitingReview(
        NoEnforcementAwaitingReviewException ex) {
        log.info("Enforcement event refused: {}", ex.getMessage());
        return ResponseEntity.ok(AboutToStartOrSubmitResponse.builder()
                                     .errors(List.of(ex.getMessage()))
                                     .build());
    }

    @ExceptionHandler(NoEnforcementException.class)
    public ResponseEntity<AboutToStartOrSubmitResponse<Object, Object>> handleNoEnforcement(NoEnforcementException ex) {
        log.info("Enforcement event refused: {}", ex.getMessage());
        return ResponseEntity.ok(AboutToStartOrSubmitResponse.builder()
                                     .errors(List.of(ex.getMessage()))
                                     .build());
    }

    @ExceptionHandler(EnforcementEventNotAvailableException.class)
    public ResponseEntity<AboutToStartOrSubmitResponse<Object, Object>> handleEventNotAvailable(
        EnforcementEventNotAvailableException ex) {
        log.info("Enforcement event refused: {}", ex.getMessage());
        return ResponseEntity.ok(AboutToStartOrSubmitResponse.builder()
                                     .errors(List.of(ex.getMessage()))
                                     .build());
    }
}
