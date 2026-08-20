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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.model.JourneyType;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.Optional;

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
    private final TaskDescriptionService taskDescriptionService;
    private final CamundaService camundaService;
    private final OrganisationService organisationService;

    @Transactional
    public RespondPossessionClaimSubmitPersistenceResult persistFinalSubmit(
        long caseReference,
        PossessionClaimResponse responseDraftData,
        PartyEntity defendantParty,
        JourneyType journeyType
    ) {
        claimResponseService.saveDraftDataForParty(responseDraftData, defendantParty, caseReference);
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

        CounterClaimEntity counterClaimEntity = savedCounterClaim.orElse(null);

        boolean paymentRequired = false;
        FeeDetails feeDetails = null;

        if (counterClaimEntity != null) {
            feeDetails = counterClaimFeeCalculator.getFeeDetails(counterClaim);

            boolean hwfReferencePresent = counterClaimFeeCalculator.isHwfReferencePresent(counterClaim);
            if (hwfReferencePresent) {
                createCounterClaimReviewWaTask(caseReference, counterClaimEntity, feeDetails);
            } else {
                paymentRequired = true;
            }
        }

        if (JourneyType.LEGAL_REPRESENTATIVE.equals(journeyType)) {
            draftCaseDataService.deleteUnsubmittedCaseData(
                caseReference,
                respondPossessionClaim,
                defendantParty.getId(),
                organisationService.getOrganisationIdForCurrentUser()
            );
        } else {
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
        }

        log.info("Successfully saved defendant response for case: {}", caseReference);

        return new RespondPossessionClaimSubmitPersistenceResult(
            responseDraftData,
            counterClaimEntity,
            feeDetails,
            paymentRequired
        );
    }

    private void createCounterClaimReviewWaTask(long caseReference,
                                                CounterClaimEntity counterClaimEntity,
                                                FeeDetails feeDetails) {

        String taskDescription = taskDescriptionService.createReviewResponseAndCounterClaimDescription(
            caseReference,
            counterClaimEntity,
            feeDetails
        );

        camundaService.createTask(
            caseReference,
            TaskType.REVIEW_DEFENDANT_RESPONSE_AND_COUNTERCLAIM,
            taskDescription);
    }

}
