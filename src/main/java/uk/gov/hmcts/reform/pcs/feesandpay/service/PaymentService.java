package uk.gov.hmcts.reform.pcs.feesandpay.service;

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
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.time.LocalDateTime;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus.PENDING;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentsClient paymentsClient;
    private final PaymentRequestMapper paymentRequestMapper;
    private final IdamService idamService;
    private final FeePaymentRepository feePaymentRepository;
    private final PcsCaseService pcsCaseService;

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
    public PaymentServiceResponse createServiceRequest(
        String caseReference,
        String ccdCaseNumber,
        FeeDetails feeDetails,
        int volume,
        String responsibleParty
    ) {
        FeeDto feeDto = paymentRequestMapper.toFeeDto(feeDetails, volume);

        CasePaymentRequestDto casePaymentRequest =
            paymentRequestMapper.toCasePaymentRequest(responsibleParty);

        CreateServiceRequestDTO requestDto = CreateServiceRequestDTO.builder()
            .callBackUrl(callbackUrl)
            .casePaymentRequest(casePaymentRequest)
            .caseReference(caseReference)
            .ccdCaseNumber(ccdCaseNumber)
            .fees(new FeeDto[]{feeDto})
            .hmctsOrgId(hmctsOrgId)
            .build();

        PaymentServiceResponse paymentServiceResponse = paymentsClient.createServiceRequest(
            idamService.getSystemUserAuthorisation(),
            requestDto
        );

        saveNewFeePayment(caseReference, feeDto, paymentServiceResponse.getServiceRequestReference(), responsibleParty);

        return paymentServiceResponse;
    }

    public void saveNewFeePayment(String caseReference, FeeDto feeDto, String serviceRequestReference,
                                  String responsibleParty) {
        ClaimEntity claimEntity = retrieveClaimEntity(caseReference);
        ClaimPartyEntity claimParty = claimEntity.getClaimParties()
            .stream()
            .filter(party -> party.getParty().getOrgName().equals(responsibleParty))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Matching PartyEntity not found"));

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .claim(claimEntity)
            .requestDate(LocalDateTime.now())
            .requestReference(serviceRequestReference)
            .amount(feeDto.getCalculatedAmount())
            .paymentStatus(PENDING)
            .party(claimParty.getParty())
            .build();

        feePaymentRepository.save(feePaymentEntity);
    }

    private ClaimEntity retrieveClaimEntity(String caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(Long.parseLong(caseReference));
        // Assuming 1 claim per PcsCase
        return  pcsCaseEntity.getClaims().getFirst();
    }

    public void processPaymentResponse(ServiceRequestUpdate serviceRequestUpdate) {
        log.info("ServiceRequestUpdate: {}", serviceRequestUpdate);
        Optional<FeePaymentEntity> byCaseReference = feePaymentRepository
            .findByRequestReference(serviceRequestUpdate.getServiceRequestReference());
        if (byCaseReference.isPresent()) {
            FeePaymentEntity feePaymentEntity = byCaseReference.get();
            feePaymentEntity.setPaymentStatus(PaymentStatus.valueOf(serviceRequestUpdate.getServiceRequestStatus()));
            feePaymentRepository.save(feePaymentEntity);
        }
    }

}
