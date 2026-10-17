package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Component
@RequiredArgsConstructor
public class DecideEnforcementSubmitHandler implements Submit<PCSCase, State> {

    private final EnforcementDecisionService enforcementDecisionService;
    private final AddressFormatter addressFormatter;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        EnforcementOrder enforcementOrder = Optional.ofNullable(pcsCase.getEnforcementOrder())
            .orElseThrow(() -> new IllegalArgumentException("No enforcement decision was made"));
        EnforcementDecision decision = Optional.ofNullable(enforcementOrder.getEnforcementDecision())
            .orElseThrow(() -> new IllegalArgumentException("No enforcement decision was made"));

        EnforcementDecisionOutcome outcome = switch (decision) {
            case ACCEPT -> enforcementDecisionService.accept(caseReference);
            case REJECT -> enforcementDecisionService.reject(caseReference,
                                                             enforcementOrder.getEnforcementRejectionReason());
        };

        String propertyAddress = addressFormatter.formatMediumAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER);
        return SubmitResponse.<State>builder()
            .confirmationBody(DecideEnforcementConfirmation.render(
                outcome, caseReference, propertyAddress, pcsCase.getCaseNameHmctsInternal()))
            .state(stateAfter(decision))
            .build();
    }

    /**
     * A rejected application puts the case back in CASE_ISSUED, the state enforceTheOrder runs in, so that the
     * claimant can apply again. An approval leaves the case state alone.
     */
    static State stateAfter(EnforcementDecision decision) {
        return decision == EnforcementDecision.REJECT ? State.CASE_ISSUED : null;
    }
}
