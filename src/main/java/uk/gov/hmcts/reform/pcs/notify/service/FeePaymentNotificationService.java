package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormDocumentGenerator.expectedClaimFormFilename;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeePaymentNotificationService {

    private final NotificationService notificationService;
    private final FeePaymentRepository feePaymentRepository;
    private final CamundaService camundaService;
    private final TranslationWAService translationWAService;

    @Transactional
    public void sendClaimantPaidCaseIssuedNotification(Integer feePaymentId) {
        FeePaymentEntity feePayment = feePaymentRepository.findById(feePaymentId)
            .orElseThrow(() -> new FeePaymentNotFoundException("Fee payment not found: " + feePaymentId));

        log.info("Sending claimant paid case issued notification for fee payment: {}", feePaymentId);

        ClaimEntity claimEntity = feePayment.getClaim();
        notificationService.sendClaimantClaimIssuedEmailNotification(claimEntity);

        PcsCaseEntity pcsCaseEntity = claimEntity.getPcsCase();

        switch (claimEntity.getLanguageUsed()) {
            case ENGLISH -> {
                if (claimEntity.getGenAppExpected() == VerticalYesNo.YES) {
                    camundaService.createTask(pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING,
                                              Duration.ofDays(1));
                } else {
                    camundaService.createTask(pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
                }
            }
            case WELSH, ENGLISH_AND_WELSH -> createTranslationTaskForClaim(pcsCaseEntity, claimEntity);
        }
    }

    private void createTranslationTaskForClaim(PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity) {
        PartyEntity claimant = pcsCaseEntity.getParties().stream()
            .filter(PartyEntity::isClaimCreator)
            .findFirst()
            .orElseThrow(() -> new PartyNotFoundException(
                "No claim creator found for case " + pcsCaseEntity.getCaseReference()));

        // The claim form is scheduled for generation so we reference it by its deterministic filename.
        List<DocumentEntity> documents = new ArrayList<>();
        documents.add(DocumentEntity.builder()
            .fileName(expectedClaimFormFilename())
            .build());

        documents.addAll(pcsCaseEntity.getDocuments().stream()
            .filter(document -> !document.isRemoved()
                && document.getClaim() != null
                && document.getClaim().getId().equals(claimEntity.getId()))
            .toList());

        translationWAService.createTranslateClaimantSubmittedDocumentTask(pcsCaseEntity, claimant, documents);
    }
}
