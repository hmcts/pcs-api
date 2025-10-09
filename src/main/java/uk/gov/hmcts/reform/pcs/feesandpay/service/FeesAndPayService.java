package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.feesandpay.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;

@Service
@Slf4j
public class FeesAndPayService {

    private final AuthTokenGenerator authTokenGenerator;
    private final FeesConfiguration feesConfiguration;
    private final FeesRegisterApi feesRegisterApi;

    public FeesAndPayService(
            AuthTokenGenerator authTokenGenerator,
            FeesConfiguration feesConfiguration,
            FeesRegisterApi feesRegisterApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.feesConfiguration = feesConfiguration;
        this.feesRegisterApi = feesRegisterApi;
    }

    /**
     * Retrieves fee information from the fees register API.
     *
     * @param feeType The type of fee to look up (e.g., "caseIssueFee")
     * @return Fee object containing code, description, version, and calculated amount
     * @throws FeeNotFoundException if the fee type is not configured
     */
    public Fee getFee(String feeType) {
        log.info("Requesting fee of type: {}", feeType);

        try {
            FeeResponse feeResponse = makeFeeRequest(feeType);
            log.info("Received fee response: {}", feeResponse);

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
     * Makes a fee lookup request to the Fees Register API using configuration data.
     * Retrieves the lookup reference data from the configuration based on the provided fee type,
     * generates a service authorization token, and calls the Fees Register API with all required parameters.
     *
     * @param feeType The type of fee to look up (must match a key in the fees configuration)
     * @return FeeResponse containing fee details from the Fees Register API
     * @throws FeeNotFoundException if the fee type is not found in the configuration
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
