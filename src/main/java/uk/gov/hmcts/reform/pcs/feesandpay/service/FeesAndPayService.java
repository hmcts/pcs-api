package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fees.client.FeesClient;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeesAndPayService {

    private final FeesConfiguration feesConfiguration;
    private final FeesClient feesClient;
    private final PaymentsClient paymentsClient;
    private final PaymentRequestMapper paymentRequestMapper;
    private final IdamService idamService;

    @Value("${payments.api.callback-url}")
    private String callbackUrl;

    @Value("${payments.params.hmctsOrgId}")
    private String hmctsOrgId;

    /**
     * Retrieves fee information from the Fees Register based on a configured fee type key.
     * The key must exist in {@link FeesConfiguration}; otherwise a {@link FeeNotFoundException} is thrown.
     *
     * @param feeTypeKey the logical fee type key (e.g., "caseIssued")
     * @return a {@link FeeLookupResponseDto} representing the fee details
     * @throws FeeNotFoundException if the fee type is not configured or the Fees Register call fails
     */
    public FeeLookupResponseDto getFee(String feeTypeKey) {
        log.debug("Requesting fee of type: {}", feeTypeKey);
        LookUpReferenceData ref = feesConfiguration.getLookup(feeTypeKey);

        if (ref == null) {
            log.error("Fee type '{}' not found in configuration", feeTypeKey);
            throw new FeeNotFoundException("Fee not found for feeType: " + feeTypeKey);
        }

        try {
            return feesClient.lookupFee(
                ref.getChannel(),
                ref.getEvent(),
                ref.getAmountOrVolume(),
                ref.getKeyword()
            );
        } catch (FeignException e) {
            log.error("Failed to retrieve fee for type: {}", feeTypeKey, e);
            throw new FeeNotFoundException("Unable to retrieve fee: " + feeTypeKey, e);
        }
    }

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
     * @param fee the fee returned from the Fees API
     * @param volume the quantity of the fee (e.g., number of items)
     * @param responsibleParty the party responsible for the payment
     * @return {@link PaymentServiceResponse} containing the service request reference
     */
    public PaymentServiceResponse createServiceRequest(
        String caseReference,
        String ccdCaseNumber,
        FeeLookupResponseDto fee,
        int volume,
        String responsibleParty
    ) {
        FeeDto feeDto = paymentRequestMapper.toFeeDto(fee, volume);

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
