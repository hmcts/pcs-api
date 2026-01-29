package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimDraftService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

/**
 * Start event handler for RespondPossessionClaim.
 *
 * <p>Orchestrates the initialization of a defendant's response:
 * 1. Checks for existing draft (returns if found)
 * 2. Loads case from database (ONCE)
 * 3. Validates defendant access
 * 4. Maps entity data to domain response
 * 5. Initializes new draft
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

        // Early return if draft already exists
        if (draftService.exists(caseReference)) {
            return draftService.load(caseReference, caseDataFromPayload);
        }

        UUID authenticatedUserId = getCurrentUserId();

        // Load case from database (ONCE - previously loaded 3 times!)
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        // Validate defendant access (throws CaseAccessException if no access)
        PartyEntity defendantEntity = accessValidator.validateAndGetDefendant(caseEntity, authenticatedUserId);

        // Map entity data to domain response
        PossessionClaimResponse initialResponse = responseMapper.mapFrom(caseEntity, defendantEntity);

        // Initialize and return draft
        return draftService.initialize(caseReference, initialResponse, caseDataFromPayload);
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(securityContextService.getCurrentUserDetails().getUid());
    }
}
