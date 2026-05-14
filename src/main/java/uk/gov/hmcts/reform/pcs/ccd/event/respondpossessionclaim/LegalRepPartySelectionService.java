package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.PossessionClaimMerger;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@AllArgsConstructor
public class LegalRepPartySelectionService {

    private final SelectedPartyRetriever selectedPartyRetriever;
    private final DefendantResponseRepository defendantResponseRepository;
    private final DraftCaseDataService draftCaseDataService;
    private final PossessionClaimResponseMapper responseMapper;
    private final PossessionClaimMerger possessionClaimMerger;

    public PCSCase getDraft(PCSCase pcsCase, List<PartyEntity> defendantPartiesLinkedAndActive, long caseReference) {
        Optional<UUID> selectedPartyId = selectedPartyRetriever.getSelectedPartyId(pcsCase);

        if (selectedPartyId.isEmpty()) {
            return buildCaseWithRepresentedPartiesOnly(pcsCase, defendantPartiesLinkedAndActive);
        }

        PartyEntity matchedDefendant = findMatchedDefendant(defendantPartiesLinkedAndActive, selectedPartyId.get());
        validateResponseNotAlreadySubmitted(caseReference, matchedDefendant.getId());

        return getDraftCaseData(caseReference, pcsCase, matchedDefendant, false);
    }

    public void validateResponseNotAlreadySubmitted(long caseReference, UUID partyId) {
        if (defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(caseReference, partyId)) {
            throw new IllegalStateException("A response has already been submitted for this case.");
        }
    }

    public PCSCase getDraftCaseData(long caseReference, PCSCase pcsCase, PartyEntity matchedDefendant,
                                     boolean singleLinkedDefendant) {

        boolean hasDraft = draftCaseDataService.hasUnsubmittedCaseData(
            caseReference,
            respondPossessionClaim,
            matchedDefendant.getId()
        );

        if (hasDraft) {
            return restoreDraft(caseReference, pcsCase, matchedDefendant, singleLinkedDefendant);
        }

        return initialiseDraft(caseReference, pcsCase, matchedDefendant);
    }

    private PartyEntity findMatchedDefendant(List<PartyEntity> parties, UUID selectedPartyId) {
        return parties.stream()
            .filter(party -> party.getId().equals(selectedPartyId))
            .findFirst()
            .orElseThrow(() -> new CaseAccessException("User is not linked as a defendant on this case"));
    }

    private PCSCase restoreDraft(long caseReference, PCSCase pcsCase, PartyEntity matchedDefendant,
                                 boolean singleLinkedDefendant) {

        PCSCase savedDraft = draftCaseDataService.getUnsubmittedCaseData(
            caseReference,
            respondPossessionClaim,
            matchedDefendant.getId()
        ).orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        PossessionClaimResponse mergedResponse = possessionClaimMerger
            .mergeLatestCaseData(pcsCase, savedDraft.getPossessionClaimResponse())
            .toBuilder()
            .claimantEnteredDefendantDetails(responseMapper.buildPartyFromEntity(matchedDefendant, pcsCase))
            .build();

        if (singleLinkedDefendant) {
            reduceCaseDefendantsToMatchedDefendant(pcsCase, matchedDefendant.getId().toString());
        } else {
            pcsCase.setAllLinkedDefendants(pcsCase.getAllDefendants());
        }

        return buildCaseWithDraft(pcsCase, mergedResponse);
    }

    private void reduceCaseDefendantsToMatchedDefendant(PCSCase pcsCase, String partyId) {
        List<ListValue<Party>> matchedDefendant = pcsCase.getAllDefendants().stream()
            .filter(party -> party.getId().equals(partyId))
            .toList();

        pcsCase.setAllLinkedDefendants(matchedDefendant);
    }


    // extract to method
    private PCSCase buildCaseWithDraft(PCSCase pcsCase, PossessionClaimResponse response) {
        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();
    }

    private PCSCase buildCaseWithRepresentedPartiesOnly(PCSCase pcsCase, List<PartyEntity> representedParties) {
        List<ListValue<Party>> representedPartyList = representedParties.stream()
            .map(this::toRepresentedPartyListValue)
            .toList();

        return pcsCase.toBuilder()
            .parties(representedPartyList)
            .allLinkedDefendants(representedPartyList)
            .possessionClaimResponse(null)
            .hasUnsubmittedCaseData(null)
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

    private PCSCase initialiseDraft(long caseReference, PCSCase pcsCase, PartyEntity defendant) {
        PossessionClaimResponse response = responseMapper.mapFrom(pcsCase, defendant);

        PCSCase draft = PCSCase.builder()
            .possessionClaimResponse(createDefendantOnlyDraft(response))
            .build();

        draftCaseDataService.patchUnsubmittedEventData(caseReference, draft, respondPossessionClaim, defendant.getId());

        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .build();
    }

    // extract to method
    protected PossessionClaimResponse createDefendantOnlyDraft(PossessionClaimResponse response) {
        return PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .build();
    }


}
