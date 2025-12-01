package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.feesandpay.config.Jurisdictions;
import uk.gov.hmcts.reform.pcs.feesandpay.config.PCSFeesClient;
import uk.gov.hmcts.reform.pcs.feesandpay.config.ServiceName;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fees.mock", havingValue = "false", matchIfMissing = true)
public class RealFeeService implements FeeService {

    private final FeesConfiguration feesConfiguration;
    private final PCSFeesClient pcsFeesClient;

    /**
     * Retrieves fee information from the Fees Register based on a configured fee type key.
     * The key must exist in {@link FeesConfiguration}; otherwise a {@link FeeNotFoundException} is thrown.
     *
     * @param serviceName the service name
     * @param jurisdictions the jurisdiction details
     * @param feeTypeKey the logical fee type key (e.g., "caseIssued")
     * @return a {@link FeeDetails} representing the fee details
     * @throws FeeNotFoundException if the fee type is not configured or the Fees Register call fails
     */
    @Override
    public FeeDetails getFee(ServiceName serviceName, Jurisdictions jurisdictions, String feeTypeKey) {
        log.debug("Requesting fee of type: {}", feeTypeKey);
        LookUpReferenceData ref = feesConfiguration.getLookup(feeTypeKey);

        if (ref == null) {
            log.error("Fee type '{}' not found in configuration", feeTypeKey);
            throw new FeeNotFoundException("Fee not found for feeType: " + feeTypeKey);
        }

        try {
            FeeLookupResponseDto feeLookupResponse = pcsFeesClient.lookupFee(serviceName, jurisdictions,
                ref.getChannel(),
                ref.getEvent(),
                ref.getAmountOrVolume(),
                ref.getKeyword()
            );

            log.debug("Successfully retrieved fee: type={}, code={}, amount={}",
                      feeTypeKey, feeLookupResponse.getCode(), feeLookupResponse.getFeeAmount());

            return FeeDetails.fromFeeLookupResponse(feeLookupResponse);
        } catch (FeignException e) {
            log.error("Failed to retrieve fee for type: {}", feeTypeKey, e);
            throw new FeeNotFoundException("Unable to retrieve fee: " + feeTypeKey, e);
        }
    }

}
