package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimDraftService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

/**
 * Start event handler for RespondPossessionClaim.
 *
 * <p>Handles two distinct flows:
 *
 * <p><b>First Time (No Draft):</b>
 * 1. Load case entity from database (case, parties, tenancy, property address)
 * 2. Validate defendant has access to this case
 * 3. Map database entities to domain objects (claimantProvided, defendantProvided)
 * 4. Create new draft in database with empty response fields
 * 5. Return populated case data to CCD UI
 *
 * <p><b>Second Time (Draft Exists):</b>
 * 1. Check if draft exists for this user
 * 2. Load saved draft from database (preserves user's previous answers)
 * 3. Return merged draft immediately (skips all DB loading and mapping)
 * 4. User continues where they left off
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final DefendantAccessValidator accessValidator;
    private final PossessionClaimResponseMapper responseMapper;
    private final RespondPossessionClaimDraftService draftService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseDataFromPayload = eventPayload.caseData();

        // SECOND TIME FLOW: If draft exists, load it and return immediately (skips all DB loading below)
        // Queries: SELECT case_data FROM draft.draft_case_data WHERE case_reference = ? AND idam_user_id = ?
        // Merges saved draft into incoming payload:
        //   - possessionClaimResponse: All user's saved data (claimantProvided + defendantProvided)
        //   - hasUnsubmittedCaseData: YES (indicates draft exists)
        //   - submitDraftAnswers: User's choice (NO for save draft, YES for final submit)
        // This preserves defendant's partially completed responses and allows them to continue where they left off
        if (draftService.exists(caseReference)) {
            return draftService.load(caseReference, caseDataFromPayload);
        }

        // FIRST TIME FLOW: No draft exists, populate from database and create new draft

        // Step 1: Load case entity with all related data (case, claims, parties, tenancy, property address)
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        // Step 2: Validate defendant has access to this case (throws CaseAccessException if no access)
        PartyEntity defendantEntity = accessValidator.validateAndGetDefendant(
            caseEntity,
            securityContextService.getCurrentUserId()
        );

        // Step 3: Map database entities to domain objects (claimantProvided, defendantProvided with empty responses)
        PossessionClaimResponse initialResponse = responseMapper.mapFrom(caseEntity, defendantEntity);

        // Step 4: Save to draft table and return populated case data
        return draftService.initialize(caseReference, initialResponse, caseDataFromPayload);
    }
}
