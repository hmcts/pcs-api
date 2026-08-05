package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.DefendantResponseStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.task.DefendantResponseSubmittedNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;
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

        CounterClaimEntity counterClaimEntity = savedCounterClaim.orElse(null);
        boolean paymentRequired = counterClaimEntity != null
            && counterClaimFeeCalculator.isPaymentRequired(counterClaim);

        if (JourneyType.LEGAL_REPRESENTATIVE.equals(journeyType)) {
            draftCaseDataService.deleteUnsubmittedCaseData(
                caseReference,
                respondPossessionClaim,
                defendantParty.getId()
            );
        } else {
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
        }

        DefendantResponseEntity defendantResponseEntity = defendantResponseService
            .saveDefendantResponse(caseReference, responseDraftData, defendantParty, journeyType);

        log.info("Successfully saved defendant response for case: {}", caseReference);

        scheduleDefendantResponseSubmittedNotification(defendantResponseEntity);
        return new RespondPossessionClaimSubmitPersistenceResult(
            responseDraftData,
            counterClaimEntity,
            paymentRequired
        );
    }

    private void scheduleDefendantResponseSubmittedNotification(DefendantResponseEntity defendantResponse) {
        if (nonNull(defendantResponse) && DefendantResponseStatus.SUBMITTED == defendantResponse.getStatus()) {
            String taskId = UUID.randomUUID().toString();

            Integer defendantResponseId = defendantResponse.getId();
            log.info(
                "Scheduling defendant response submitted notification for: {}, with task id: {}",
                defendantResponseId,
                taskId
            );

            schedulerClient.scheduleIfNotExists(
                DefendantResponseSubmittedNotificationTaskComponent.DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR
                    .instance(taskId)
                    .data(DefendantResponseStatusChangeTaskData.builder()
                              .defendantResponseId(defendantResponseId)
                              .build())
                    .scheduledTo(Instant.now())
            );
        }
    }
}
