package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.RequestHolder;
import uk.gov.hmcts.reform.pcs.ccd.service.party.SolicitorForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@AllArgsConstructor
@Slf4j
public class LegalRepresentativeCaseDraftLoader {

    private final PcsCaseService pcsCaseService;
    private final PossessionClaimResponseMapper responseMapper;
    private final DraftCaseDataService draftCaseDataService;
    private final SolicitorForDefendantAccessValidator solicitorForDefendantAccessValidator;
    private final SecurityContextService securityContextService;
    private final RequestHolder requestHolder;

    private final DraftCaseDataRepository draftCaseDataRepository;

    public PCSCase loadDraft(long caseReference, PCSCase pcsCase) {
        List<PartyEntity> parties = loadAndValidateDefendantForSolicitor(caseReference);
        String partyId = requestHolder.getHeader("Client-context");

        boolean isSpecificDefendant = partyId != null;


        // refactor to make a specific party via FE
        PartyEntity matchedDefendant = parties.getFirst();
        isSpecificDefendant = draftCaseDataService.hasUnsubmittedCaseData(caseReference,
                                                                          respondPossessionClaim,
                                                                          matchedDefendant.getId());

        if (isSpecificDefendant) {
            // PartyEntity matchedDefendant = parties.stream().filter(p ->
            // p.getId().toString().equals(partyId)).findFirst()
            // .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

            return restoreSavedDraftAnswersForLegalRepresentative(caseReference, pcsCase, matchedDefendant);
        } else {
            initializeDraftForAllDefendants(caseReference, pcsCase, parties);
        }

        return pcsCase;
    }

    private List<PartyEntity> loadAndValidateDefendantForSolicitor(long caseReference) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        return solicitorForDefendantAccessValidator.validateAndGetDefendants(caseEntity,
                                                                             securityContextService.getCurrentUserId());
    }

    private void initializeDraftForAllDefendants(long caseReference, PCSCase pcsCase, List<PartyEntity> parties) {
        parties.forEach(party -> initializeDraftForDefendant(caseReference, pcsCase, party));
    }

    private void initializeDraftForDefendant(long caseReference, PCSCase pcsCase, PartyEntity party) {
        PossessionClaimResponse response = responseMapper.mapFrom(pcsCase, party);
        createInitialDraft(caseReference, party.getId(), response);
    }

    private PCSCase restoreSavedDraftAnswersForLegalRepresentative(long caseReference, PCSCase pcsCase,
                                                                   PartyEntity matchedDefendant) {
        PCSCase savedDraft = loadSavedDraft(caseReference, matchedDefendant.getId());
        Party claimantEnteredDetails = responseMapper.buildPartyFromEntity(matchedDefendant, pcsCase);

        PossessionClaimResponse mergedResponse = buildMergedResponse(
            pcsCase,
            savedDraft.getPossessionClaimResponse(),
            claimantEnteredDetails
        );

        return buildCaseWithDraft(pcsCase, mergedResponse);
    }

    private PCSCase buildCaseWithDraft(PCSCase pcsCase, PossessionClaimResponse response) {
        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();
    }

    private PCSCase loadSavedDraft(long caseReference, UUID partyId) {
        return draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim, partyId)
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
     *
     * @param latestCase Latest case data from CCD (has up-to-date claimant info)
     * @param savedResponses Saved defendant responses from draft
     * @return Merged response with latest claimant orgs and saved defendant answers
     */
    private PossessionClaimResponse mergeLatestCaseData(PCSCase latestCase,
                                                        PossessionClaimResponse savedResponses) {
        List<ListValue<String>> latestClaimantOrgs = createClaimantOrgNameList(latestCase);

        return savedResponses.toBuilder()
            .claimantOrganisations(latestClaimantOrgs)
            .build();
    }

    private void createInitialDraft(long caseReference, UUID partyId, PossessionClaimResponse response) {
        // Filter to ONLY defendant's editable fields (exclude claimantOrganisations)
        PossessionClaimResponse defendantFieldsOnly = PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .build();  // claimantOrganisations intentionally excluded - view data only

        PCSCase draftWithOnlyResponseData = PCSCase.builder()
            .possessionClaimResponse(defendantFieldsOnly)
            .build();

        draftCaseDataService.patchUnsubmittedEventDataForLegalRepresentativeDefendant(
            caseReference,
            draftWithOnlyResponseData,
            respondPossessionClaim,
            partyId
        );
    }
}
