package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class GenAppPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final GenAppRepository genAppRepository;
    private final GenAppDocumentGenerator genAppDocumentGenerator;

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        UUID genAppId = feePaymentEntity.getRelatedEntityId();

        log.info("Handling callback for gen app payment for gen app ID {}", genAppId);


        if (feePaymentEntity.getPaymentStatus() == PaymentStatus.PAID) {
            GenAppEntity genAppEntity = findGenAppEntity(genAppId);
            if (genAppEntity.getState() != GenAppState.GEN_APP_ISSUED) {
                genAppEntity.setState(GenAppState.GEN_APP_ISSUED);
                long caseReference = genAppEntity.getPcsCase().getCaseReference();
                genAppDocumentGenerator.createSubmissionDocument(caseReference, genAppEntity);
                // TODO: Send email notification here (HDPI-4297)

            } else {
                log.warn("Gen app {} was already in GEN_APP_ISSUED state. Ignoring payment callback", genAppId);
            }
        } else {
            log.warn("The payment was not successful [{}] for gen app {} on case {}",
                     feePaymentEntity.getPaymentStatus(), genAppId, paymentStatusCallback.getCcdCaseNumber());
        }
    }

    private GenAppEntity findGenAppEntity(UUID genAppId) {
        return genAppRepository.findById(genAppId)
            .orElseThrow(() -> new GenAppNotFoundException("Unable to find gen app with ID " + genAppId));
    }

}
