package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.task.PendingCounterClaimIssuedNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Service
@Slf4j
@RequiredArgsConstructor
public class RespondPossessionClaimSubmitService {

    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final CounterClaimService counterClaimService;
    private final CounterClaimFeeCalculator counterClaimFeeCalculator;
    private final DocumentService documentService;
    private final DraftCaseDataService draftCaseDataService;
    private final SchedulerClient schedulerClient;

    @Transactional
    public RespondPossessionClaimSubmitPersistenceResult persistFinalSubmit(
        long caseReference,
        PossessionClaimResponse responseDraftData,
        PartyEntity defendantParty,
        JourneyType journeyType
    ) {
        claimResponseService.saveDraftDataForParty(responseDraftData, defendantParty);
        defendantResponseService.saveDefendantResponse(caseReference, responseDraftData, defendantParty, journeyType);

        DefendantResponses defendantResponses = responseDraftData.getDefendantResponses();
        CounterClaim counterClaim = defendantResponses.getCounterClaim();
        Optional<CounterClaimEntity> savedCounterClaim =
            counterClaimService.saveCounterClaim(caseReference, counterClaim, defendantParty);

        savedCounterClaim.ifPresent(counterClaimEntity -> documentService.createCounterClaimUploadedDocuments(
            defendantResponses.getCounterClaimDocuments(),
            counterClaimEntity,
            counterClaimEntity.getPcsCase(),
            counterClaimEntity.getParty()
        ));

        if (JourneyType.LEGAL_REPRESENTATIVE.equals(journeyType)) {
            draftCaseDataService.deleteUnsubmittedCaseData(
                caseReference,
                respondPossessionClaim,
                defendantParty.getId()
            );
        } else {
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
        }

        boolean paymentRequired = false;

        CounterClaimEntity counterClaimEntity = savedCounterClaim.orElse(null);
        if (counterClaimEntity != null) {
            paymentRequired = counterClaimFeeCalculator.isPaymentRequired(counterClaim);
            schedulePendingCounterClaimIssuedNotification(counterClaimEntity);
        }

        log.info("Successfully saved defendant response for case: {}", caseReference);

        return new RespondPossessionClaimSubmitPersistenceResult(
            responseDraftData,
            counterClaimEntity,
            paymentRequired
        );
    }

    private void schedulePendingCounterClaimIssuedNotification(CounterClaimEntity counterClaimEntity) {
        if (CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED == counterClaimEntity.getStatus()) {
            String taskId = UUID.randomUUID().toString();
            UUID counterClaimId = counterClaimEntity.getId();
            log.info(
                "Scheduling pending counter claim issued notification for: {}, with task id: {}",
                counterClaimId,
                taskId
            );

            schedulerClient.scheduleIfNotExists(
                PendingCounterClaimIssuedNotificationTaskComponent.PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR
                    .instance(taskId)
                    .data(CounterClaimTaskData.builder()
                              .counterClaimId(counterClaimId)
                              .build())
                    .scheduledTo(Instant.now())
            );
        }
    }
}
