package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeesAndPayService {

    private final AuthTokenGenerator authTokenGenerator;
    private final FeesConfiguration feesConfiguration;
    private final FeesRegisterApi feesRegisterApi;
    private final IdamService idamService;
    private final PaymentsClient paymentsClient;
    private final PaymentRequestMapper paymentRequestMapper;

    private static final String ACTION_PAYMENT = "payment";

    @Value("${payments.api.callback-url}")
    private String callbackUrl;

    @Value("${payments.params.hmctsOrgId}")
    private String hmctsOrgId;

    /**
     * Retrieves fee information from the Fees Register based on a configured fee type.
     * The feeType must exist in {@link FeesConfiguration}; otherwise, a {@link FeeNotFoundException} is thrown.
     * Any Feign-related issues while calling the remote API are caught and rethrown as {@link FeeNotFoundException}.
     *
     * @param feeType the logical fee type key as defined in {@link FeesConfiguration}
     * @return a {@link Fee} containing the code, description, version and calculated amount
     * @throws FeeNotFoundException if the fee type is not configured or the Fees Register call fails
     */
    public Fee getFee(String feeType) {
        log.info("Requesting fee of type: {}", feeType);
        try {
            FeeResponse feeResponse = makeFeeRequest(feeType);
            log.debug("Received fee response: {}", feeResponse);

            return Fee.builder()
                .code(feeResponse.getCode())
                .description(feeResponse.getDescription())
                .version(feeResponse.getVersion())
                .calculatedAmount(feeResponse.getFeeAmount())
                .build();
        } catch (FeignException e) {
            log.error("Failed to retrieve fee for type: {}", feeType, e);
            throw new FeeNotFoundException("Unable to retrieve fee: " + feeType, e);
        }
    }

    /**
     * Creates a service request in the Payments API for the given case and fee details.
     * This method:
     * <ol>
     *     <li>Maps the provided {@link Fee} and volume to a {@link FeeDto}.</li>
     *     <li>Builds a {@link CasePaymentRequestDto} with a standard action and responsible party.</li>
     *     <li>Constructs a {@link CreateServiceRequestDTO} including callback URL and HMCTS org ID.</li>
     *     <li>Calls {@link PaymentsClient#createServiceRequest(String, CreateServiceRequestDTO)}
     *     using the system user token.</li>
     * </ol>
     * Any exception during the process is logged and rethrown, including Feign-specific details if available.
     *
     * @param caseReference the business case reference to be sent to the Payments API
     * @param ccdCaseNumber the CCD case number to be sent to the Payments API
     * @param fee the fee details to be included
     * @param volume the quantity of the fee (e.g. number of items)
     * @param responsibleParty the party responsible for the payment
     * @return {@link PaymentServiceResponse} containing the newly created service request reference
     * @throws RuntimeException if an error occurs while constructing or submitting the request
     */
    public PaymentServiceResponse createServiceRequest(
        String caseReference,
        String ccdCaseNumber,
        Fee fee,
        int volume,
        String responsibleParty
    ) {
        try {
            FeeDto feeDto = paymentRequestMapper.toFeeDto(fee, volume);

            CasePaymentRequestDto casePaymentRequest =
                paymentRequestMapper.toCasePaymentRequest(ACTION_PAYMENT, responsibleParty);

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

        } catch (Exception e) {
            log.error("Exception occurred in createServiceRequest: {}", e.getMessage(), e);
            if (e instanceof FeignException fe) {
                log.error("Feign error details - Status: {}, Body: {}", fe.status(), fe.contentUTF8());
            }
            throw e;
        }
    }

    /**
     * Performs the remote call to the Fees Register using configuration for the supplied fee type.
     * Resolves {@link LookUpReferenceData} from {@link FeesConfiguration} and invokes the lookup API
     * with a freshly generated service-to-service token.
     *
     * @param feeType the logical fee type key as defined in {@link FeesConfiguration}
     * @return {@link FeeResponse} as returned by the Fees Register
     * @throws FeeNotFoundException if the fee type is not present in configuration
     */
    private FeeResponse makeFeeRequest(String feeType) {
        LookUpReferenceData feeFromReferenceData = feesConfiguration.getFees().get(feeType);

        if (feeFromReferenceData == null) {
            log.error("Fee type '{}' not found in configuration", feeType);
            throw new FeeNotFoundException("Fee not found for feeType: " + feeType);
        }

        return feesRegisterApi.lookupFee(
            authTokenGenerator.generate(),
            feeFromReferenceData.getService(),
            feeFromReferenceData.getJurisdiction1(),
            feeFromReferenceData.getJurisdiction2(),
            feeFromReferenceData.getChannel(),
            feeFromReferenceData.getEvent(),
            feeFromReferenceData.getApplicantType(),
            feeFromReferenceData.getAmountOrVolume(),
            feeFromReferenceData.getKeyword()
        );
    }
}
