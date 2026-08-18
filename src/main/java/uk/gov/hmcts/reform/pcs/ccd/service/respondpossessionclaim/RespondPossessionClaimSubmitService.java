package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PreIssueChecklistCode;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PreIssueChecklistEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.preissuechecklist.PreIssueChecklistService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

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
    private final TranslationWAService translationWAService;
    private final PreIssueChecklistService preIssueChecklistService;

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

        createTranslationTaskForResponse(savedResponse, defendantParty);

        CounterClaimEntity counterClaimEntity = savedCounterClaim.orElse(null);
        boolean paymentRequired = counterClaimEntity != null
            && counterClaimFeeCalculator.isPaymentRequired(counterClaim);

        if (counterClaimEntity != null && !paymentRequired) {
            counterClaimEntity.setStatus(CounterClaimState.PENDING_REVIEW);
            createTranslationTaskForCounterClaim(
                counterClaimEntity, counterClaimDocuments, savedResponse, defendantParty);
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

    private void createTranslationTaskForResponse(DefendantResponseEntity savedResponse,
                                                  PartyEntity defendantParty) {

        LanguageUsed languageUsed = savedResponse.getLanguageUsed();
        if (languageUsed != LanguageUsed.WELSH && languageUsed != LanguageUsed.ENGLISH_AND_WELSH) {
            return;
        }

        PcsCaseEntity pcsCaseEntity = defendantParty.getPcsCase();
        List<DocumentEntity> documents = pcsCaseEntity.getDocuments().stream()
            .filter(document -> !document.isRemoved())
            .filter(document -> isDefendantResponseDocument(document, savedResponse))
            .toList();

        if (!documents.isEmpty()) {
            preIssueChecklistService.save(PreIssueChecklistEntity.builder()
                .code(PreIssueChecklistCode.TRANSLATE_DEFENDANT_DOCUMENT)
                .allowManualCompletion(true)
                .response(savedResponse)
                .build());
        }

        translationWAService.createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, defendantParty, documents);
    }

    private boolean isDefendantResponseDocument(DocumentEntity document,
                                                DefendantResponseEntity savedResponse) {
        return document.getDefendantResponse() != null
            && document.getDefendantResponse().getId().equals(savedResponse.getId());
    }

    private void createTranslationTaskForCounterClaim(CounterClaimEntity counterClaimEntity,
                                                       List<DocumentEntity> counterClaimDocuments,
                                                       DefendantResponseEntity savedResponse,
                                                       PartyEntity defendantParty) {

        LanguageUsed languageUsed = savedResponse.getLanguageUsed();
        if (languageUsed != LanguageUsed.WELSH && languageUsed != LanguageUsed.ENGLISH_AND_WELSH) {
            return;
        }

        List<DocumentEntity> documents = counterClaimDocuments.stream()
            .filter(document -> !document.isRemoved())
            .toList();

        if (documents.isEmpty()) {
            return;
        }

        preIssueChecklistService.save(PreIssueChecklistEntity.builder()
            .code(PreIssueChecklistCode.TRANSLATE_DEFENDANT_DOCUMENT)
            .allowManualCompletion(true)
            .counterClaim(counterClaimEntity)
            .build());

        translationWAService.createTranslateDefendantSubmittedDocumentTask(defendantParty.getPcsCase(), defendantParty,
                                                                           documents);
    }

}
