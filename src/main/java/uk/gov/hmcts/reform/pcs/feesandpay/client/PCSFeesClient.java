package uk.gov.hmcts.reform.pcs.feesandpay.client;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.math.BigDecimal;

@Service
@Slf4j
@AllArgsConstructor
public class PCSFeesClient {

    private final FeesConfiguration feesConfiguration;
    private final FeesApi feesApi;

    public FeeLookupResponseDto lookupFee(FeeType feeType) {
        return lookupFee(feeType, null);
    }

    public FeeLookupResponseDto lookupFee(FeeType feeType, BigDecimal amountOrVolume) {
        FeesConfiguration.LookUpReferenceData ref = feesConfiguration.getLookup(feeType);

        if (ref == null) {
            log.error("Fee type '{}' not found in configuration", feeType);
            throw new FeeNotFoundException("Fee not found for feeType: " + feeType);
        }

        BigDecimal resolvedAmountOrVolume = amountOrVolume != null ? amountOrVolume : ref.getAmountOrVolume();

        return feesApi.lookupFee(
            ref.getService(),
            ref.getJurisdiction1(),
            ref.getJurisdiction2(),
            ref.getChannel(),
            ref.getEvent(),
            ref.getApplicantType(),
            resolvedAmountOrVolume,
            ref.getKeyword()
        );
    }

}
