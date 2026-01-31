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
 * - First time: Load case from DB, map entities, create draft, return to UI
 * - Second time: Load saved draft, merge with CCD payload, return to UI
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
        PCSCase caseWithMetadata = eventPayload.caseData();  // Contains case reference, state, dates from CCD

        // Second time: Draft exists - restore defendant's saved answers
        if (draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim)) {
            return restoreSavedDraftAnswers(caseReference, caseWithMetadata);
        }

        // First time: Build from database entities

        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity defendantEntity = accessValidator.validateAndGetDefendant(
            caseEntity,
            securityContextService.getCurrentUserId()
        );

        // Map database entities to response structure (claimantProvided + defendantProvided)
        PossessionClaimResponse responseFromDatabase = responseMapper.mapFrom(caseEntity, defendantEntity);

        // Save to draft table so defendant can come back later
        createInitialDraft(caseReference, responseFromDatabase);

        // Return case with response structure for UI
        return caseWithMetadata.toBuilder()
            .possessionClaimResponse(responseFromDatabase)
            .build();
    }

    // Second time flow: Load defendant's saved answers and add them back into the case
    // Why merge? Case has metadata (reference, state, dates), draft has defendant's saved answers
    private PCSCase restoreSavedDraftAnswers(long caseReference, PCSCase caseWithMetadata) {
        PCSCase savedDraft = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new IllegalStateException(
                "Draft not found for case " + caseReference
            ));

        // Combine: case metadata (base) + saved draft answers (overlay)
        return caseWithMetadata.toBuilder()
            .possessionClaimResponse(savedDraft.getPossessionClaimResponse())  // Defendant's saved answers
            .hasUnsubmittedCaseData(YesOrNo.YES)                                // Flag: has draft
            .build();  // Everything else (state, reference, dates) from caseWithMetadata
    }

    // First time flow: Create initial draft so defendant can save progress
    private void createInitialDraft(long caseReference, PossessionClaimResponse responseFromDatabase) {
        PCSCase draftWithOnlyResponseData = PCSCase.builder()
            .possessionClaimResponse(responseFromDatabase)
            .build();  // Only response data - no metadata (CCD owns that)

        draftCaseDataService.patchUnsubmittedEventData(
            caseReference,
            draftWithOnlyResponseData,
            respondPossessionClaim
        );
    }
}
