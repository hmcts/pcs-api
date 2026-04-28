package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimParty;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

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

        PCSCase pcsCase = eventPayload.caseData();
        PartyEntity matchedDefendant = loadAndValidateDefendant(caseReference);

        if (hasDraftInProgress(caseReference)) {
            return restoreSavedDraftAnswers(caseReference, pcsCase, matchedDefendant);
        }

        return initializeFirstTimeResponse(caseReference, pcsCase, matchedDefendant);
    }

    private PartyEntity loadAndValidateDefendant(long caseReference) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        return accessValidator.validateAndGetDefendant(caseEntity, securityContextService.getCurrentUserId());
    }

    private boolean hasDraftInProgress(long caseReference) {
        return draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim);
    }

    private PCSCase initializeFirstTimeResponse(long caseReference, PCSCase pcsCase, PartyEntity matchedDefendant) {
        PossessionClaimResponse response = responseMapper.mapFrom(pcsCase, matchedDefendant);
        createInitialDraft(caseReference, response);

        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .build();
    }

    private PCSCase restoreSavedDraftAnswers(long caseReference, PCSCase pcsCase, PartyEntity matchedDefendant) {
        PCSCase savedDraft = loadSavedDraft(caseReference);
        Party claimantEnteredDetails = responseMapper.buildPartyFromEntity(matchedDefendant, pcsCase);

        PossessionClaimResponse mergedResponse = buildMergedResponse(
            pcsCase,
            savedDraft.getPossessionClaimResponse(),
            claimantEnteredDetails
        );

        return buildCaseWithDraft(pcsCase, mergedResponse);
    }

    private PCSCase loadSavedDraft(long caseReference) {
        return draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));
    }

    private PossessionClaimResponse buildMergedResponse(PCSCase latestCase,
                                                         PossessionClaimResponse savedResponses,
                                                         Party claimantEnteredDetails) {
        return mergeLatestCaseData(latestCase, savedResponses)
            .toBuilder()
            .claimantEnteredDefendantDetails(claimantEnteredDetails)
            .build();
    }

    private PCSCase buildCaseWithDraft(PCSCase pcsCase, PossessionClaimResponse response) {
        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();
    }

    /**
     * Creates a list of claimant organisation names from case claimant parties.
     * Transforms Party objects into simple organisation name strings.
     *
     * @param pcsCase View-populated case with allClaimants from CCD
     * @return List of claimant organisation names, or empty list if none found
     */
    private List<ListValue<String>> createClaimantOrgNameList(PCSCase pcsCase) {
        List<ListValue<Party>> allClaimants = pcsCase.getAllClaimants();

        if (allClaimants == null || allClaimants.isEmpty()) {
            log.warn("No claimant parties found in case, returning empty organisation list");
            return List.of();
        }

        return allClaimants.stream()
            .map(claimant -> ListValue.<String>builder()
                .id(claimant.getId())
                .value(claimant.getValue().getOrgName())
                .build())
            .toList();
    }

    /**
     * Merges latest case data with saved defendant responses.
     * Takes fresh claimant organisations from latest case and combines with saved defendant answers.
     * Builds a list of claim parties with their roles.
     *
     * @param latestCase Latest case data from CCD (has up-to-date claimant info)
     * @param savedResponses Saved defendant responses from draft
     * @return Merged response with latest claimant orgs and saved defendant answers
     */
    private PossessionClaimResponse mergeLatestCaseData(PCSCase latestCase,
                                                         PossessionClaimResponse savedResponses) {
        List<ListValue<String>> latestClaimantOrgs = createClaimantOrgNameList(latestCase);
        List<ListValue<ClaimParty>> latestClaimParties = responseMapper.buildClaimParties(latestCase);

        return savedResponses.toBuilder()
            .claimantOrganisations(latestClaimantOrgs)
            .claimParties(latestClaimParties)
            .build();
    }

    /**
     * Creates initial draft in draft table so defendant can save progress.
     *
     * <p>Only saves defendant contact details and responses (editable defendant data). Claim data
     * comes from view classes on each START callback. Case metadata (state, dates) is managed by
     * CCD, not stored in draft.
     *
     * <p><b>IMPORTANT:</b> claimantOrganisations is intentionally excluded from the draft.
     * It's view-only data that comes fresh from CCD allClaimants on each START callback.
     */
    private void createInitialDraft(long caseReference, PossessionClaimResponse response) {
        // Filter to ONLY defendant's editable fields (exclude claimantOrganisations)
        PossessionClaimResponse defendantFieldsOnly = PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .build();  // claimantOrganisations intentionally excluded - view data only

        PCSCase draftWithOnlyResponseData = PCSCase.builder()
            .possessionClaimResponse(defendantFieldsOnly)
            .build();

        draftCaseDataService.patchUnsubmittedEventData(
            caseReference,
            draftWithOnlyResponseData,
            respondPossessionClaim
        );
    }
}
