package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimantMakeAnApplication;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private static final String MAKE_AN_APPLICATION_URL = "/cases/case-details/%d/trigger/%s";

    private final DraftCaseDataService draftCaseDataService;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
        setMakeAnApplicationUrl(caseReference, state, pcsCase);
    }

    private void setHasEnforcementDraftData(long caseReference, State state, PCSCase pcsCase) {
        if (State.CASE_ISSUED == state) {
            boolean hasUnsubmittedCaseData = draftCaseDataService
                .hasUnsubmittedCaseData(caseReference, enforceTheOrder);
            if (hasUnsubmittedCaseData) {
                Optional<PCSCase> unsubmittedCaseData = draftCaseDataService
                    .getUnsubmittedCaseData(caseReference, enforceTheOrder);
                unsubmittedCaseData.ifPresent(inflated ->
                                                  pcsCase.setEnforcementOrder(inflated.getEnforcementOrder()));
                pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.YES);
            } else {
                pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.NO);
            }
        }
    }

    // Event pages cannot resolve ${[CASE_REFERENCE]}, so the link to make a general application is built here
    private void setMakeAnApplicationUrl(long caseReference, State state, PCSCase pcsCase) {
        if (State.CASE_ISSUED == state) {
            if (pcsCase.getEnforcementOrder() == null) {
                pcsCase.setEnforcementOrder(EnforcementOrder.builder().build());
            }
            pcsCase.getEnforcementOrder().setMakeAnApplicationUrl(
                MAKE_AN_APPLICATION_URL.formatted(caseReference, claimantMakeAnApplication.name()));
        }
    }
}
