package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

/**
 * Start event handler for RespondPossessionClaim.
 *
 * <p>Two flows:
 * - First time: Use view-populated PCSCase (claim data), load defendant entity for matching, create draft
 * - Second time: Load saved draft, merge with view-populated PCSCase, return to UI
 *
 * <p>Claim data (tenancy, rent, notices) comes from view classes (TenancyLicenceView, RentDetailsView, etc.)
 * and is already in eventPayload.caseData(). Only defendant's editable contact details need initialization.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final SecurityContextService securityContextService;
    private final CitizenCaseDraftLoader citizenCaseDraftLoader;
    private final LegalRepresentativeCaseDraftLoader legalRepresentativeCaseDraftLoader;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.info("RespondPossessionClaim start callback invoked for Case Reference: {}", caseReference);

        PCSCase pcsCase = eventPayload.caseData();
        if (securityContextService.getCurrentUserDetails().getRoles().contains(UserRole.CITIZEN.getRole())) {
            return citizenCaseDraftLoader.loadDraft(caseReference, pcsCase);
        } else {
            return legalRepresentativeCaseDraftLoader.loadDraft(caseReference, pcsCase);
        }
    }

}
