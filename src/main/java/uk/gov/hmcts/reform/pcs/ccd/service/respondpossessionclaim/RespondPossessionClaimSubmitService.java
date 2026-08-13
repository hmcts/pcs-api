package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.util.ArrayList;
import java.util.List;
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
    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;

    @Transactional
    public RespondPossessionClaimSubmitPersistenceResult persistFinalSubmit(
        long caseReference,
        PossessionClaimResponse responseDraftData,
        PartyEntity defendantParty,
        JourneyType journeyType
    ) {
        claimResponseService.saveDraftDataForParty(responseDraftData, defendantParty);
        DefendantResponseEntity savedResponse = defendantResponseService.saveDefendantResponse(
            caseReference, responseDraftData, defendantParty, journeyType);

        DefendantResponses defendantResponses = responseDraftData.getDefendantResponses();
        CounterClaim counterClaim = defendantResponses.getCounterClaim();
        Optional<CounterClaimEntity> savedCounterClaim =
            counterClaimService.saveCounterClaim(caseReference, counterClaim, defendantParty);

        List<DocumentEntity> counterClaimDocuments = savedCounterClaim
            .map(counterClaimEntity -> documentService.createCounterClaimUploadedDocuments(
                defendantResponses.getCounterClaimDocuments(),
                counterClaimEntity,
                counterClaimEntity.getPcsCase(),
                counterClaimEntity.getParty()
            ))
            .orElse(List.of());

        CounterClaimEntity counterClaimEntity = savedCounterClaim.orElse(null);
        boolean paymentRequired = counterClaimEntity != null
            && counterClaimFeeCalculator.isPaymentRequired(counterClaim);

        createTranslateDefendantDocumentTask(caseReference, savedResponse, counterClaimDocuments,
                                             paymentRequired, defendantParty);

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

    private void createTranslateDefendantDocumentTask(long caseReference,
                                                      DefendantResponseEntity savedResponse,
                                                      List<DocumentEntity> counterClaimDocuments,
                                                      boolean counterClaimPaymentRequired,
                                                      PartyEntity defendantParty) {

        LanguageUsed languageUsed = savedResponse.getLanguageUsed();
        if (languageUsed != LanguageUsed.WELSH && languageUsed != LanguageUsed.ENGLISH_AND_WELSH) {
            return;
        }

        PcsCaseEntity pcsCaseEntity = defendantParty.getPcsCase();
        List<DocumentEntity> documents = new ArrayList<>(pcsCaseEntity.getDocuments().stream()
            .filter(document -> !document.isRemoved())
            .filter(document -> isDefendantResponseDocument(document, savedResponse))
            .toList());

        // Counterclaim documents are only translatable once the counterclaim fee (if any) has been paid;
        // when payment is still pending they're picked up later via CounterClaimPaymentCallbackHandler
        if (!counterClaimPaymentRequired) {
            counterClaimDocuments.stream()
                .filter(document -> !document.isRemoved())
                .forEach(documents::add);
        }

        if (documents.isEmpty()) {
            return;
        }

        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        String description = taskDescriptionService.createTranslateDefendantDocumentDescription(
            caseReference, mainClaim, defendantParty, documents);

        camundaService.createTask(caseReference, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, description);
    }

    private static boolean isDefendantResponseDocument(DocumentEntity document,
                                                        DefendantResponseEntity savedResponse) {
        return document.getDefendantResponse() != null
            && document.getDefendantResponse().getId().equals(savedResponse.getId());
    }

}
