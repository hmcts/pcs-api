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
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@AllArgsConstructor
@Slf4j
public class LegalRepresentativeCaseDraftLoader {

    private final PcsCaseService pcsCaseService;
    private final PossessionClaimResponseMapper responseMapper;
    private final DraftCaseDataService draftCaseDataService;
    private final DefendantResponseRepository defendantResponseRepository;
    private final LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    private final SecurityContextService securityContextService;

    public PCSCase loadDraft(long caseReference, PCSCase pcsCase) {
        List<PartyEntity> parties = loadAndValidateDefendantsForLegalRep(caseReference);
        Optional<UUID> selectedPartyId = getSelectedPartyId(pcsCase);

        if (selectedPartyId.isEmpty()) {
            return buildCaseWithRepresentedPartiesOnly(pcsCase, parties);
        }

        PartyEntity matchedDefendant = findMatchedDefendant(parties, selectedPartyId.get());
        validateResponseNotAlreadySubmitted(caseReference, matchedDefendant.getId());

        if (draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim,
                                                        matchedDefendant.getId())) {
            return restoreSavedDraftAnswersForLegalRepresentative(caseReference, pcsCase, matchedDefendant);
        }

        return initializeFirstTimeResponse(caseReference, pcsCase, matchedDefendant);
    }

    private List<PartyEntity> loadAndValidateDefendantsForLegalRep(long caseReference) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        return legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity,
            securityContextService.getCurrentUserId());
    }

    private PCSCase buildCaseWithRepresentedPartiesOnly(PCSCase pcsCase, List<PartyEntity> representedParties) {
        List<ListValue<Party>> representedPartyList = representedParties.stream()
            .map(this::toRepresentedPartyListValue)
            .toList();

        return pcsCase.toBuilder()
            .parties(representedPartyList)
            .allDefendants(representedPartyList)
            .possessionClaimResponse(null)
            .hasUnsubmittedCaseData(null)
            .build();
    }

    private PCSCase initializeFirstTimeResponse(long caseReference, PCSCase pcsCase, PartyEntity party) {
        PossessionClaimResponse response = responseMapper.mapFrom(pcsCase, party);
        createInitialDraft(caseReference, party.getId(), response);
        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .build();
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

    private ListValue<Party> toRepresentedPartyListValue(PartyEntity partyEntity) {
        Party party = Party.builder()
            .firstName(partyEntity.getFirstName())
            .lastName(partyEntity.getLastName())
            .orgName(partyEntity.getOrgName())
            .build();

        return ListValue.<Party>builder()
            .id(partyEntity.getId().toString())
            .value(party)
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

        draftCaseDataService.patchUnsubmittedEventData(
            caseReference,
            draftWithOnlyResponseData,
            respondPossessionClaim,
            partyId
        );
    }

    private void validateResponseNotAlreadySubmitted(long caseReference, UUID partyId) {
        if (defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(caseReference, partyId)) {
            throw new IllegalStateException("A response has already been submitted for this case.");
        }
    }

    private Optional<UUID> getSelectedPartyId(PCSCase pcsCase) {
        String selectedPartyId = pcsCase.getSelectedRespondingPartyId();
        if (isBlank(selectedPartyId)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(selectedPartyId));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid selected responding party id for respond to claim", ex);
        }
    }

    private PartyEntity findMatchedDefendant(List<PartyEntity> parties, UUID selectedPartyId) {
        return parties.stream()
            .filter(party -> party.getId().equals(selectedPartyId))
            .findFirst()
            .orElseThrow(() -> new CaseAccessException("User is not linked as a defendant on this case"));
    }
}
