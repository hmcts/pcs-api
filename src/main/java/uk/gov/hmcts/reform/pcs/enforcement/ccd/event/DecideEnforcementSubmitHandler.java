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
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

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
            return errorResponse(List.of(NO_DECISION_MESSAGE));
        }
        String rejectionReason = enforcementOrder.getEnforcementRejectionReason();
        if (decision == EnforcementDecision.REJECT && StringUtils.isBlank(rejectionReason)) {
            return errorResponse(List.of(EnforcementDecisionService.NO_REJECTION_REASON_MESSAGE));
        }

        EnforcementChange<EnforcementDecisionOutcome> decided = switch (decision) {
            case ACCEPT -> enforcementDecisionService.accept(caseReference);
            case REJECT -> enforcementDecisionService.reject(caseReference, rejectionReason);
        };
        if (decided.refused()) {
            return errorResponse(decided.errors());
        }
        EnforcementDecisionOutcome outcome = decided.change();

        String propertyAddress = addressFormatter.formatMediumAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER);
        return SubmitResponse.<State>builder()
            .confirmationHeader(DecideEnforcementConfirmation.header(outcome))
            .confirmationBody(DecideEnforcementConfirmation.body(
                outcome, caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference), propertyAddress,
                pcsCase.getCaseNameHmctsInternal()))
            .state(State.CASE_ISSUED)
            .build();
    }

    private static SubmitResponse<State> errorResponse(List<String> errors) {
        return SubmitResponse.<State>builder().errors(errors).build();
    }
}
