package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.LegalRepHelper;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimSubmitConfirmationService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitPersistenceResult;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegalRepSubmissionEventStrategy implements RespondPossessionClaimSubmissionEventStrategy {

    private final DraftCaseDataService draftCaseDataService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final PartyService partyService;
    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final PcsCaseService pcsCaseService;
    private final SelectedPartyRetriever selectedPartyRetriever;
    private final SubmitResponseFactory submitResponseFactory;
    private final RespondPossessionClaimSubmitService respondPossessionClaimSubmitService;
    private final CounterClaimSubmitConfirmationService counterClaimSubmitConfirmationService;
    private final SecurityContextService securityContextService;
    private final LegalRepHelper legalRepHelper;

    @Override
    public boolean supports(List<String> roles) {
        return !roles.contains(UserRole.CITIZEN.getRole());
    }

    @Override
    public SubmitResponse<State> process(EventPayload<PCSCase, State> eventPayload) {
        if (securityContextService.getCurrentUserId() == null) {
            log.error("Current user IDAM ID is null");
            throw new IllegalStateException("Current user IDAM ID is null");
        }

        Long caseReference = eventPayload.caseReference();
        UUID representedPartyId = selectedPartyRetriever
            .getCurrentRepresentedPartyId(eventPayload.caseData())
            .orElseThrow(() -> new IllegalStateException("No selected responding party id for respond to claim"));

        PCSCase draftData = draftCaseDataService
            .getUnsubmittedCaseData(caseReference, respondPossessionClaim, representedPartyId)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        PossessionClaimResponse responseDraftData = draftData.getPossessionClaimResponse();

        Optional<SubmitResponse<State>> validationResult = submitResponseFactory
            .validate(responseDraftData, caseReference);

        if (validationResult.isPresent()) {
            return validationResult.get();
        }


        PartyEntity defendantParty = partyService.getPartyEntityById(representedPartyId, caseReference);

        RespondPossessionClaimSubmitPersistenceResult persistenceResult = respondPossessionClaimSubmitService
            .persistFinalSubmit(caseReference, responseDraftData, defendantParty, JourneyType.LEGAL_REPRESENTATIVE);

        LegalRepresentativeEntity legalRepresentativeEntity =
            legalRepresentativeRepository
                .findByPartyLinkedToLegalRepresentativeAndActive(representedPartyId)
                .orElseThrow(IllegalAccessError::new);

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        PartyEntity legalRepresentativePartyEntity = partyService.getPartyEntityByEntityId(representedPartyId, caseReference);

        DefendantResponseEntity defendantResponse = pcsCaseEntity.getDefendantResponses().stream()
            .filter(counter -> counter.getParty().getId().equals(representedPartyId))
            .findFirst()
            .orElse(null);

        SubmitResponse<State> submitResponse =  counterClaimSubmitConfirmationService
            .buildSubmitResponse(caseReference, persistenceResult, defendantParty);

        // Schedule this as a task
        legalRepHelper.submit(legalRepresentativeEntity, pcsCaseEntity, legalRepresentativePartyEntity, defendantResponse);

        return submitResponse;
    }
}
