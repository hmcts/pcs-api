package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimSubmitConfirmationService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitPersistenceResult;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegalRepSubmissionEventStrategy implements RespondPossessionClaimSubmissionEventStrategy {

    private final DraftCaseDataService draftCaseDataService;
    private final SelectedPartyRetriever selectedPartyRetriever;
    private final SubmitResponseFactory submitResponseFactory;
    private final PartyService partyService;
    private final OrganisationRepository organisationRepository;
    private final PcsCaseService pcsCaseService;
    private final RespondPossessionClaimSubmitService respondPossessionClaimSubmitService;
    private final CounterClaimSubmitConfirmationService counterClaimSubmitConfirmationService;
    private final SecurityContextService securityContextService;
    private final OrganisationService organisationService;
    private final NotificationService notificationService;

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
        PCSCase pcsCase = eventPayload.caseData();
        UUID representedPartyId = selectedPartyRetriever
            .getCurrentRepresentedPartyId(eventPayload.caseData())
            .or(() -> selectedPartyRetriever.getSelectedPartyId(pcsCase))
            .orElseThrow(() -> new IllegalStateException("No selected responding party id for respond to claim"));

        String organisationId = organisationService.getOrganisationIdForCurrentUser();

        PCSCase draftData = draftCaseDataService
            .getUnsubmittedCaseData(
                caseReference, respondPossessionClaim, representedPartyId,
                organisationId
            )
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

        OrganisationEntity organisationEntity =
            organisationRepository
                .findByPartyLinkedToOrganisationAndCaseAndActive(representedPartyId, caseReference)
                .orElseThrow();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        DefendantResponseEntity defendantResponse = pcsCaseEntity.getDefendantResponses().stream()
            .filter(counter -> counter.getParty().getId().equals(representedPartyId))
            .findFirst()
            .orElse(null);

        SubmitResponse<State> submitResponse = counterClaimSubmitConfirmationService
            .buildSubmitResponse(caseReference, persistenceResult, defendantParty);

        // Schedule this as a task
        sendNotification(organisationEntity, pcsCaseEntity, defendantResponse, defendantParty);

        return submitResponse;
    }

    private boolean isHwfBlank(CounterClaimEntity counterClaim) {
        return isBlank(counterClaim.getHwfReferenceNumber());
    }

    private void sendNoCounterClaimNotification(OrganisationEntity
                                                    organisationEntity, PcsCaseEntity pcsCaseEntity,
                                                DefendantResponseEntity defendantResponse) {
        notificationService
            .sendDefendantResponseConfirmationToLegalRepresentativeNoCounterClaim(
                organisationEntity,
                pcsCaseEntity,
                defendantResponse
        );
    }

    private void sendCounterClaimNoPaymentRequiredNotification(
        OrganisationEntity organisationEntity,
        PcsCaseEntity pcsCaseEntity, DefendantResponseEntity defendantResponse) {
        notificationService
            .sendDefendantResponseConfirmationToLegalRepresentativeNoPaymentRequired(
                organisationEntity, pcsCaseEntity, defendantResponse);
    }

    private void sendCounterClaimPaymentRequiredNotification(
        OrganisationEntity organisationEntity,
        PcsCaseEntity pcsCaseEntity, DefendantResponseEntity defendantResponse) {
        notificationService
            .sendDefendantResponseConfirmationToLegalRepresentativePaymentRequired(
                organisationEntity, pcsCaseEntity, defendantResponse);
    }

    private void sendNotification(OrganisationEntity organisationEntity,
                                  PcsCaseEntity pcsCaseEntity, DefendantResponseEntity defendantResponse,
                                  PartyEntity defendantParty) {
        Optional<CounterClaimEntity> counterClaimEntityOptional = pcsCaseEntity.getCounterClaims().stream()
            .filter(counterClaimEntity -> defendantParty.getId()
                .equals(counterClaimEntity.getParty().getId())).findFirst();

        counterClaimEntityOptional
            .ifPresentOrElse(
                counterClaimEntity -> sendCounterClaimNotification(
                    counterClaimEntity,
                    organisationEntity,
                    pcsCaseEntity, defendantResponse
                ),
                () ->
                    sendNoCounterClaimNotification(
                        organisationEntity, pcsCaseEntity, defendantResponse)
        );
    }

    private void sendCounterClaimNotification(CounterClaimEntity counterClaimEntity,
                                              OrganisationEntity
                                                  organisationEntity, PcsCaseEntity pcsCaseEntity,
                                              DefendantResponseEntity defendantResponse) {
        if (counterClaimEntity.getStatus() == CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED) {
            if (!isHwfBlank(counterClaimEntity)) {
                // email notification for the “Response and counterclaim submitted"
                // RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED
                sendCounterClaimNoPaymentRequiredNotification(
                    organisationEntity,
                    pcsCaseEntity, defendantResponse
                );
            } else {
                //  email notification for "Response submitted - payment required for your counterclaim"
                //  RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED
                sendCounterClaimPaymentRequiredNotification(
                    organisationEntity,
                    pcsCaseEntity, defendantResponse
                );
            }
        }
    }
}
