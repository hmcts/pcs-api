package uk.gov.hmcts.reform.pcs.feesandpay.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.client.PCSFeesClient;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fees.mock", havingValue = "false", matchIfMissing = true)
public class RealFeeService implements FeeService {

    private final PCSFeesClient pcsFeesClient;

    /**
     * Retrieves fee information from the Fees Register based on a configured fee type key.
     * The key must exist in {@link FeesConfiguration}; otherwise a {@link FeeNotFoundException} is thrown.
     *
     * @param feeType the logical fee type key (e.g., "caseIssued")
     * @return a {@link FeeDetails} representing the fee details
     * @throws FeeNotFoundException if the fee type is not configured or the Fees Register call fails
     */
    @Override
    public FeeDetails getFee(FeeType feeType) {
        log.debug("Requesting fee of type: {}", feeType);
        try {
            FeeLookupResponseDto feeLookupResponse = pcsFeesClient.lookupFee(feeType);
            log.debug("Successfully retrieved fee: type={}, code={}, amount={}",
                      feeType, feeLookupResponse.getCode(), feeLookupResponse.getFeeAmount());

            return FeeDetails.fromFeeLookupResponse(feeLookupResponse);
        } catch (FeignException e) {
            log.error("Failed to retrieve fee for type: {}", feeType, e);
            throw new FeeNotFoundException("Unable to retrieve fee: " + feeType, e);
        }
    }

}
