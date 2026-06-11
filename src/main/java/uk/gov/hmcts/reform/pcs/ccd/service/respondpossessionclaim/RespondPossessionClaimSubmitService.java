package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;

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

    @Transactional
    public RespondPossessionClaimSubmitPersistenceResult persistFinalSubmit(
        long caseReference,
        PossessionClaimResponse responseDraftData
    ) {
        claimResponseService.saveDraftData(responseDraftData, caseReference);
        defendantResponseService.saveDefendantResponse(caseReference, responseDraftData);

        DefendantResponses defendantResponses = responseDraftData.getDefendantResponses();
        CounterClaim counterClaim = defendantResponses.getCounterClaim();
        Optional<CounterClaimEntity> savedCounterClaim =
            counterClaimService.saveCounterClaim(caseReference, counterClaim);

        savedCounterClaim.ifPresent(counterClaimEntity -> saveCounterClaimDocuments(
            defendantResponses,
            counterClaimEntity
        ));

        CounterClaimEntity counterClaimEntity = savedCounterClaim.orElse(null);
        boolean issuedWithoutPayment = false;

        if (counterClaimEntity != null
            && !counterClaimFeeCalculator.isPaymentRequired(counterClaim)) {
            counterClaimEntity = counterClaimService.issueCounterClaim(counterClaimEntity);
            issuedWithoutPayment = true;
        }

        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
        log.info("Successfully saved defendant response for case: {}", caseReference);

        return new RespondPossessionClaimSubmitPersistenceResult(
            responseDraftData,
            counterClaimEntity,
            issuedWithoutPayment
        );
    }

    private void saveCounterClaimDocuments(DefendantResponses defendantResponses,
                                           CounterClaimEntity counterClaimEntity) {
        if (CollectionUtils.isEmpty(defendantResponses.getCounterClaimDocuments())) {
            return;
        }

        documentService.createCounterClaimUploadedDocuments(
            defendantResponses.getCounterClaimDocuments(),
            counterClaimEntity,
            counterClaimEntity.getPcsCase(),
            counterClaimEntity.getParty()
        );
    }
}
