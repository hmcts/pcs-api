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
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimSubmitConfirmationService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitPersistenceResult;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
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
    private final PartyService partyService;
    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final PcsCaseService pcsCaseService;
    private final SelectedPartyRetriever selectedPartyRetriever;
    private final SubmitResponseFactory submitResponseFactory;
    private final RespondPossessionClaimSubmitService respondPossessionClaimSubmitService;
    private final CounterClaimSubmitConfirmationService counterClaimSubmitConfirmationService;
    private final SecurityContextService securityContextService;
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

        DefendantResponseEntity defendantResponse = pcsCaseEntity.getDefendantResponses().stream()
            .filter(counter -> counter.getParty().getId().equals(representedPartyId))
            .findFirst()
            .orElse(null);

        SubmitResponse<State> submitResponse =  counterClaimSubmitConfirmationService
            .buildSubmitResponse(caseReference, persistenceResult, defendantParty);

        // Schedule this as a task
        Optional<CounterClaimEntity> counterClaimEntityOptional = getCounterClaim(pcsCaseEntity);

        if (counterClaimEntityOptional.isEmpty()) {
            if (defendantResponse != null && DefendantResponseStatus.SUBMITTED == defendantResponse.getStatus()) {
                noCounterClaim(legalRepresentativeEntity, pcsCaseEntity, legalRepresentativePartyEntity,
                               defendantResponse);
            }
        } else {
            CounterClaimEntity counterClaimEntity = counterClaimEntityOptional.get();

            if (counterClaimEntity.getStatus() == CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED) {

                if (!isHwfBlank(counterClaimEntity)) {
//                  email notification for the “Response and counterclaim submitted"
//                  RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED
                    counterClaimSuccessNoPaymentRequired(legalRepresentativeEntity, pcsCaseEntity, defendantResponse);
                } else {
//                  email notification for "Response submitted - payment required for your counterclaim"
//                  RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED
                    counterClaimSuccessPaymentRequired(legalRepresentativeEntity, pcsCaseEntity, defendantResponse);
                }
            } else {
//          (isHwfBlank(counterClaimEntity) && isCounterClaimIssued(counterClaimEntity))
//          email notification for "Response submitted - payment Success"
            }
        }

        return submitResponse;
    }

    private Optional<CounterClaimEntity> getCounterClaim(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity == null
            || pcsCaseEntity.getCounterClaims() == null
            || pcsCaseEntity.getCounterClaims().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pcsCaseEntity.getCounterClaims().getFirst());
    }

    private boolean isCounterClaimIssued(CounterClaimEntity counterClaim) {
        return CounterClaimState.COUNTER_CLAIM_ISSUED == counterClaim.getStatus();
    }

    private boolean isHwfBlank(CounterClaimEntity counterClaim) {
        return isBlank(counterClaim.getHwfReferenceNumber());
    }


    private EmailNotificationResponse noCounterClaim(LegalRepresentativeEntity legalRepresentativeEntity, PcsCaseEntity pcsCaseEntity,
                                                     DefendantResponseEntity defendantResponse) {
        return notificationService
            .sendDefendantResponseConfirmationToLegalRepresentativeNoCounterClaim(legalRepresentativeEntity,
                                                                                                 pcsCaseEntity,
                                                                                                 defendantResponse);
    }

    private EmailNotificationResponse counterClaimPaymentSuccess(LegalRepresentativeEntity legalRepresentativeEntity, PcsCaseEntity pcsCaseEntity,
                                                                 DefendantResponseEntity defendantResponse) {
        return notificationService
            .sendDefendantResponseConfirmationToLegalRepresentativePaymentSuccess(legalRepresentativeEntity,
                                                                                  pcsCaseEntity,
                                                                                  defendantResponse);
    }

    private EmailNotificationResponse counterClaimPaymentRequired(LegalRepresentativeEntity legalRepresentativeEntity, PcsCaseEntity pcsCaseEntity,
                                                                  DefendantResponseEntity defendantResponse) {
        return notificationService
            .sendDefendantResponseConfirmationToLegalRepresentativePaymentRequired(legalRepresentativeEntity,
                                                                                  pcsCaseEntity,
                                                                                  defendantResponse);
    }

    private EmailNotificationResponse counterClaimNoPaymentRequired(LegalRepresentativeEntity legalRepresentativeEntity, PcsCaseEntity pcsCaseEntity,
                                                                    DefendantResponseEntity defendantResponse) {
        return notificationService
            .sendDefendantResponseConfirmationToLegalRepresentativeNoPaymentRequired(legalRepresentativeEntity,
                                                                                   pcsCaseEntity,
                                                                                   defendantResponse);
    }

    private EmailNotificationResponse pickTemplate(LegalRepresentativeEntity legalRepresentativeEntity, PcsCaseEntity pcsCaseEntity,
                                                   DefendantResponseEntity defendantResponse) {
        Optional<CounterClaimEntity> counterClaimEntityOptional = getCounterClaim(pcsCaseEntity);

        if (counterClaimEntityOptional.isEmpty()) {
            if (defendantResponse != null && DefendantResponseStatus.SUBMITTED == defendantResponse.getStatus()) {
                noCounterClaim(legalRepresentativeEntity, pcsCaseEntity, legalRepresentativePartyEntity,
                               defendantResponse);
            }
        } else {
            CounterClaimEntity counterClaimEntity = counterClaimEntityOptional.get();

            if (counterClaimEntity.getStatus() == CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED) {

                if (!isHwfBlank(counterClaimEntity)) {
//                  email notification for the “Response and counterclaim submitted"
//                  RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED
                    counterClaimSuccessNoPaymentRequired(legalRepresentativeEntity, pcsCaseEntity,
                                                 legalRepresentativePartyEntity, defendantResponse);
                } else {
//                  email notification for "Response submitted - payment required for your counterclaim"
//                  RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED
                    counterClaimSuccessPaymentRequired(legalRepresentativeEntity, pcsCaseEntity,
                                                       legalRepresentativePartyEntity, defendantResponse);
                }
            } else {
//          (isHwfBlank(counterClaimEntity) && isCounterClaimIssued(counterClaimEntity))
//          email notification for "Response submitted - payment Success"

            }
        }
    }
}
