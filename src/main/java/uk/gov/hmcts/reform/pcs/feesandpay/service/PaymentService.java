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
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentsClient paymentsClient;
    private final PaymentRequestMapper paymentRequestMapper;
    private final IdamService idamService;

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

        return paymentsClient.createServiceRequest(
            idamService.getSystemUserAuthorisation(),
            requestDto
        );
    }
}
