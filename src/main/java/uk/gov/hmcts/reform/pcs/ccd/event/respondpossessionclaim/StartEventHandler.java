package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

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

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final DefendantAccessValidator accessValidator;
    private final PossessionClaimResponseMapper responseMapper;
    private final DraftCaseDataService draftCaseDataService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.info("RespondPossessionClaim start callback invoked for Case Reference: {}", caseReference);

        PCSCase pcsCase = eventPayload.caseData();  // Already has view-populated claim data

        // Restore existing draft if defendant has already started responding
        if (draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim)) {
            return restoreSavedDraftAnswers(caseReference, pcsCase);
        }

        // First time: Load case entity only for access validation and matched defendant
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity matchedDefendant = accessValidator.validateAndGetDefendant(
            caseEntity,
            securityContextService.getCurrentUserId()
        );

        // Initialize only defendant's editable contact details
        // Claim data already visible in pcsCase via CitizenAccess
        PossessionClaimResponse response = responseMapper.mapFrom(pcsCase, matchedDefendant);

        // Save initial draft (only defendant contact details and responses, not claim data)
        createInitialDraft(caseReference, response);

        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .build();
    }

    /**
     * Restores defendant's draft answers from previous session.
     *
     * <p>Merges two sources:
     * - pcsCase: View-populated claim data (fresh from view classes)
     * - savedDraft: Defendant's saved answers from draft table
     *
     * <p>Sets hasUnsubmittedCaseData=YES so UI shows "Continue" button instead of "Start"
     */
    private PCSCase restoreSavedDraftAnswers(long caseReference, PCSCase pcsCase) {
        PCSCase savedDraft = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new IllegalStateException(
                "Draft not found for case " + caseReference
            ));

        return pcsCase.toBuilder()
            .possessionClaimResponse(savedDraft.getPossessionClaimResponse())
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();
    }

    /**
     * Creates initial draft in draft table so defendant can save progress.
     *
     * <p>Only saves defendant contact details and responses (editable defendant data). Claim data
     * comes from view classes on each START callback. Case metadata (state, dates) is managed by
     * CCD, not stored in draft.
     */
    private void createInitialDraft(long caseReference, PossessionClaimResponse response) {
        PCSCase draftWithOnlyResponseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        draftCaseDataService.patchUnsubmittedEventData(
            caseReference,
            draftWithOnlyResponseData,
            respondPossessionClaim
        );
    }
}
