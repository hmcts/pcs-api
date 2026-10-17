package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;

@Component
@RequiredArgsConstructor
public class DecideEnforcementStartHandler implements Start<PCSCase, State> {

    private final EnforcementDecisionService enforcementDecisionService;
    private final AddressFormatter addressFormatter;

    /**
     * Sets the header fields and clears any earlier decision.
     *
     * @throws NoEnforcementAwaitingReviewException if no application is awaiting review, which CCD shows as an error
     */
    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        if (!enforcementDecisionService.hasEnforcementAwaitingReview(eventPayload.caseReference())) {
            throw new NoEnforcementAwaitingReviewException();
        }
        PCSCase pcsCase = eventPayload.caseData();
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        enforcementOrder.setFormattedPropertyAddress(
            addressFormatter.formatMediumAddress(pcsCase.getPropertyAddress(), BR_DELIMITER));
        enforcementOrder.setFormattedCaseName(pcsCase.getCaseNameHmctsInternal());
        enforcementOrder.setEnforcementDecision(null);
        enforcementOrder.setEnforcementRejectionReason(null);
        return pcsCase;
    }
}
