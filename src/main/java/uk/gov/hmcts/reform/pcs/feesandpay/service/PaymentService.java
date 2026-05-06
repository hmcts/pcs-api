package uk.gov.hmcts.reform.pcs.feesandpay.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentsClient paymentsClient;
    private final PaymentRequestMapper paymentRequestMapper;
    private final IdamService idamService;
    private final FeePaymentRepository feePaymentRepository;
    private final PcsCaseService pcsCaseService;
    private final NotificationService notificationService;

    private static final String CASE_ISSUED = "CASE_ISSUED";

    @Value("${payments.api.callback-url}")
    private String callbackUrl;

    @Value("${payments.params.hmctsOrgId}")
    private String hmctsOrgId;

    /**
     * Creates a service request in the Payments API for the given case and fee details.
     * Steps:
     * 1) Maps the provided fee and volume to a Payments {@link FeeDto}.
     * 2) Builds a {@link CasePaymentRequestDto}.
     * 3) Constructs a {@link CreateServiceRequestDTO} including callback URL and HMCTS org ID.
     * 4) Calls {@link PaymentsClient#createServiceRequest(String, CreateServiceRequestDTO)} using the system user
     * token.
     *
     * @param caseReference the business case reference sent to the Payments API
     * @param ccdCaseNumber the CCD case number sent to the Payments API
     * @param feeDetails the fee details
     * @param volume the quantity of the fee (e.g., number of items)
     * @param responsibleParty the party responsible for the payment
     * @return {@link PaymentServiceResponse} containing the service request reference
     */
    @Transactional
    public PaymentServiceResponse createServiceRequest(String caseReference, String ccdCaseNumber,
                                                       FeeDetails feeDetails, int volume, String responsibleParty) {
        log.info("""
                Building payload for caseReference: {}, ccdCaseNumber: {} \
                feeDetails: {}, volume: {}, responsibleParty: {}""",
                caseReference, ccdCaseNumber, feeDetails, volume, responsibleParty);
        FeeDto feeDto = paymentRequestMapper.toFeeDto(feeDetails, volume);
        CasePaymentRequestDto casePaymentRequest = paymentRequestMapper.toCasePaymentRequest(responsibleParty);
        log.info("casePaymentRequest: {}", casePaymentRequest);

        CreateServiceRequestDTO requestDto = CreateServiceRequestDTO.builder()
            .callBackUrl(callbackUrl)
            .casePaymentRequest(casePaymentRequest)
            .caseReference(caseReference)
            .ccdCaseNumber(ccdCaseNumber)
            .fees(new FeeDto[]{feeDto})
            .hmctsOrgId(hmctsOrgId)
            .build();

        log.info("Calling ServiceCreateRequest with callback url: {} using hmctsOrgId: {} for caseReference: {}",
                 callbackUrl, hmctsOrgId, caseReference);
        PaymentServiceResponse paymentServiceResponse = paymentsClient.createServiceRequest(
            idamService.getSystemUserAuthorisation(), requestDto);

        ClaimEntity claimEntity = retrieveClaimEntity(Long.parseLong(caseReference));
        log.info("Response received for caseReference: {} - Response : {}", caseReference, paymentServiceResponse);
        saveNewFeePayment(caseReference, claimEntity, feeDto,
                          paymentServiceResponse.getServiceRequestReference());

        return paymentServiceResponse;
    }

    @Transactional
    public void processPaymentResponse(ServiceRequestUpdate serviceRequestUpdate) {
        log.info("ServiceRequestUpdate status: {}", serviceRequestUpdate.getServiceRequestStatus());
        Optional<FeePaymentEntity> byCaseReference = feePaymentRepository
            .findByRequestReference(serviceRequestUpdate.getServiceRequestReference());

        if (byCaseReference.isEmpty()) {
            log.error("Unable to find a payment with the service request reference : {}",
                      serviceRequestUpdate.getServiceRequestReference());
            return;
        }


        FeePaymentEntity feePaymentEntity = byCaseReference.get();
        feePaymentEntity.setExternalReference(serviceRequestUpdate.getPayment().getPaymentReference());
        feePaymentEntity.setPaymentStatus(PaymentStatus.fromValue(serviceRequestUpdate.getServiceRequestStatus()));
        feePaymentRepository.save(feePaymentEntity);

        if (PaymentStatus.PAID.equals(feePaymentEntity.getPaymentStatus())) {
            sendNotificationOnFeePaymentSuccess(feePaymentEntity);
        }
    }

    @Transactional
    public void saveNewFeePayment(String caseReference, ClaimEntity claimEntity,
                                   FeeDto feeDto, String serviceRequestReference) {
        log.info("Saving New Fee Payment for the case: {} with serviceRequestReference: {}", caseReference,
                 serviceRequestReference);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .claim(claimEntity)
            .requestReference(serviceRequestReference)
            .amount(feeDto.getCalculatedAmount())
            .build();
        feePaymentRepository.save(feePaymentEntity);
    }

    private ClaimEntity retrieveClaimEntity(Long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        // Assuming 1 claim per PcsCase
        return pcsCaseEntity.getClaims().getFirst();
    }

    private void sendNotificationOnFeePaymentSuccess(FeePaymentEntity feePayment) {
        ClaimEntity claim = feePayment.getClaim();
        PartyEntity defendant = claim.getClaimParties()
            .stream()
            .filter(cp -> PartyRole.DEFENDANT.equals(cp.getRole()))
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElse(null);

        if (defendant == null) {
            log.warn("No defendant found for claim {}", claim.getId());
            return;
        }

        CounterClaimEntity counterClaim = claim.getPcsCase().getCounterClaims()
            .stream()
            .filter(counter -> counter.getParty().getId().equals(defendant.getId()))
            .findFirst()
            .orElse(null);

        if (counterClaim == null) {
            log.warn("No counterclaim found for claim {}", claim.getId());
            return;
        }

        DefendantResponseEntity defendantResponse = claim.getPcsCase().getDefendantResponses().stream()
            .filter(counter -> counter.getParty().getId().equals(defendant.getId()))
            .findFirst()
            .orElse(null);

        if (defendantResponse == null) {
            log.warn("No defendant response found for claim {}", claim.getId());
            return;
        }

        if (!CASE_ISSUED.equals(counterClaim.getStatus())) {
            log.info("Counterclaim status not eligible for AC04 email: {}", counterClaim.getStatus());
            return;
        }

        log.info("Sending counterclaim payment success email for claim {}", claim.getId());
        notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
    }

}
