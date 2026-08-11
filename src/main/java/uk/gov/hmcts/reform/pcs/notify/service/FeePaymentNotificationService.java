package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeePaymentNotificationService {

    private final NotificationService notificationService;
    private final FeePaymentRepository feePaymentRepository;
    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final PartyService partyService;

    @Transactional
    public void sendClaimantPaidCaseIssuedNotification(Integer feePaymentId) {
        FeePaymentEntity feePayment = feePaymentRepository.findById(feePaymentId)
            .orElseThrow(() -> new FeePaymentNotFoundException("Fee payment not found: " + feePaymentId));

        log.info("Sending claimant paid case issued notification for fee payment: {}", feePaymentId);

        ClaimEntity claimEntity = feePayment.getClaim();
        notificationService.sendClaimantClaimIssuedEmailNotification(claimEntity);

        PcsCaseEntity pcsCaseEntity = claimEntity.getPcsCase();

        switch (claimEntity.getLanguageUsed()) {
            case ENGLISH -> camundaService.createTask(
                pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
            case WELSH, ENGLISH_AND_WELSH -> createTranslateClaimantDocumentTask(
                pcsCaseEntity.getCaseReference(), claimEntity);
        }
    }

    private void createTranslateClaimantDocumentTask(long caseReference, ClaimEntity claimEntity) {
        List<DocumentEntity> documents = claimEntity.getPcsCase().getDocuments().stream()
            .filter(document -> document.getClaim() != null
                && document.getClaim().getId().equals(claimEntity.getId()))
            .toList();

        if (!documents.isEmpty()) {
            PartyEntity primaryClaimant = partyService.getPrimaryClaimantPartyEntity(claimEntity.getPcsCase());
            String partyLabel = partyService.getPartyLabel(claimEntity, primaryClaimant.getId());

            String description = taskDescriptionService.createTranslateClaimantDocumentDescription(
                caseReference, documents, partyLabel, false);
            camundaService.createTask(caseReference, TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT, description);
        }
    }
}
