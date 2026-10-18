package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

/**
 * Records the caseworker's decision. Everything that stops the decision being recorded is returned as an error
 * on the response: EnforcementCallbackExceptionHandler cannot be scoped to a decentralised submit, and an
 * exception out of a transactional service would also mark the SDK's event transaction rollback-only.
 */
@Component
@RequiredArgsConstructor
public class DecideEnforcementSubmitHandler implements Submit<PCSCase, State> {

    static final String NO_DECISION_MESSAGE = "No enforcement decision was made";

    private final EnforcementDecisionService enforcementDecisionService;
    private final AddressFormatter addressFormatter;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        EnforcementDecision decision = Optional.ofNullable(enforcementOrder)
            .map(EnforcementOrder::getEnforcementDecision)
            .orElse(null);
        if (decision == null) {
            return errorResponse(NO_DECISION_MESSAGE);
        }
        String rejectionReason = enforcementOrder.getEnforcementRejectionReason();
        if (decision == EnforcementDecision.REJECT && StringUtils.isBlank(rejectionReason)) {
            return errorResponse(EnforcementDecisionService.NO_REJECTION_REASON_MESSAGE);
        }

        Optional<EnforcementDecisionOutcome> outcome = switch (decision) {
            case ACCEPT -> enforcementDecisionService.accept(caseReference);
            case REJECT -> enforcementDecisionService.reject(caseReference, rejectionReason);
        };
        if (outcome.isEmpty()) {
            // Another caseworker decided the same application while this event was open.
            return errorResponse(NoEnforcementAwaitingReviewException.MESSAGE);
        }

        String propertyAddress = addressFormatter.formatMediumAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER);
        // Either decision returns the case to CASE_ISSUED. PENDING_ENFORCEMENT_ISSUED, which enforceTheOrder
        // moves the case to, offers no events of its own, so a case left there after an approval can neither
        // confirm the eviction nor use any core event; the enforcement's own status tracks its progress.
        return SubmitResponse.<State>builder()
            .confirmationHeader(DecideEnforcementConfirmation.header(outcome.get()))
            .confirmationBody(DecideEnforcementConfirmation.body(
                outcome.get(), caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference), propertyAddress,
                pcsCase.getCaseNameHmctsInternal()))
            .state(State.CASE_ISSUED)
            .build();
    }

    private static SubmitResponse<State> errorResponse(String error) {
        return SubmitResponse.<State>builder().errors(List.of(error)).build();
    }
}
