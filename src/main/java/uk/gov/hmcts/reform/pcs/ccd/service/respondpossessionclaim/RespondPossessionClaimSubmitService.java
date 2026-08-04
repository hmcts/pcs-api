package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Service
@Slf4j
@RequiredArgsConstructor
public class RespondPossessionClaimSubmitService {

    private final PcsCaseService pcsCaseService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final CounterClaimService counterClaimService;
    private final CounterClaimFeeCalculator counterClaimFeeCalculator;
    private final DocumentService documentService;
    private final DraftCaseDataService draftCaseDataService;
    private final TaskDescriptionService taskDescriptionService;
    private final CamundaService camundaService;

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

        CounterClaimEntity counterClaimEntity = savedCounterClaim.orElse(null);
        boolean hwfReferencePresent = counterClaimFeeCalculator.isHwfReferencePresent(counterClaim);

        boolean paymentRequired = false;

        if (counterClaimEntity != null) {

            List<DocumentEntity> counterClaimDocuments = documentService.createCounterClaimUploadedDocuments(
                defendantResponses.getCounterClaimDocuments(),
                counterClaimEntity,
                counterClaimEntity.getPcsCase(),
                counterClaimEntity.getParty()
            );

            if (hwfReferencePresent) {
                createCounterclaimReviewWaTask(caseReference, counterClaimEntity, counterClaimDocuments);
            } else {
                paymentRequired = true;
            }
        }

        if (JourneyType.LEGAL_REPRESENTATIVE.equals(journeyType)) {
            draftCaseDataService.deleteUnsubmittedCaseData(
                caseReference,
                respondPossessionClaim,
                defendantParty.getId()
            );
        } else {
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
        }

        log.info("Successfully saved defendant response for case: {}", caseReference);

        return new RespondPossessionClaimSubmitPersistenceResult(
            responseDraftData,
            counterClaimEntity,
            paymentRequired
        );
    }

    private void createCounterclaimReviewWaTask(long caseReference,
                                                CounterClaimEntity counterClaimEntity,
                                                List<DocumentEntity> counterClaimDocuments) {

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        String taskDescription = taskDescriptionService.createReviewResponseAndCounterclaimDescription(
            caseReference,
            pcsCaseEntity.getMainClaim(),
            counterClaimEntity.getParty(),
            counterClaimDocuments
        );

        camundaService.createTask(
            caseReference,
            TaskType.REVIEW_DEFENDANT_RESPONSE_AND_COUNTERCLAIM,
            taskDescription);
    }

}
