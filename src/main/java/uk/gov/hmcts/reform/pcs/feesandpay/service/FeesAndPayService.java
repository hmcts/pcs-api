package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.feesandpay.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.feesandpay.api.PaymentApi;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestBody;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestResponse;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeesAndPayService {

    private final AuthTokenGenerator authTokenGenerator;
    private final FeesConfiguration feesConfiguration;
    private final FeesRegisterApi feesRegisterApi;
    private final IdamService idamService;
    private final PaymentApi paymentApi;
    private final PaymentRequestMapper paymentRequestMapper;

    private static final String ACTION_PAYMENT = "payment";

    @Value("${payment.callback-url}") private String callbackUrl;

    /**
     * Retrieves fee information from the fees register API.
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

    public ServiceRequestResponse createServiceRequest(
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

            ServiceRequestBody requestBody = paymentRequestMapper.toServiceRequestBody(
                callbackUrl,
                caseReference,
                ccdCaseNumber,
                new FeeDto[]{feeDto},
                casePaymentRequest
            );

            return paymentApi.createServiceRequest(
                idamService.getSystemUserAuthorisation(),
                authTokenGenerator.generate(),
                requestBody
            );

        } catch (Exception e) {
            log.error("Exception occurred in createServiceRequest: {}", e.getMessage(), e);
            if (e instanceof FeignException fe) {
                log.error("Feign error details - Status: {}, Body: {}", fe.status(), fe.contentUTF8());
            }
            throw e;
        }
    }

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
