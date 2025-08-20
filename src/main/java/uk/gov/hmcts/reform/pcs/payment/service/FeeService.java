package uk.gov.hmcts.reform.pcs.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.payment.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.payment.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.payment.config.FeesConfiguration.LookUpReferenceData;
import uk.gov.hmcts.reform.pcs.payment.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.model.FeeResponse;

@Service
@Slf4j
public class FeeService {

    private final FeesConfiguration feesConfiguration;
    private final FeesRegisterApi feesRegisterApi;

    public FeeService(FeesConfiguration feesConfiguration, FeesRegisterApi feesRegisterApi) {
        this.feesConfiguration = feesConfiguration;
        this.feesRegisterApi = feesRegisterApi;
    }

    public Fee getFeeWithoutHearing() {
        FeeResponse feeResponse = makeRequest();

        return new Fee(
            feeResponse.getCode(),
            feeResponse.getDescription(),
            feeResponse.getVersion(),
            feeResponse.getAmount()
        );
    }

    private FeeResponse makeRequest() {
        LookUpReferenceData lookUpReferenceData = feesConfiguration.getFees().get("feeWithoutHearing");

        return feesRegisterApi.findFee(
            lookUpReferenceData.getChannel(),
            lookUpReferenceData.getEvent(),
            lookUpReferenceData.getJurisdiction1(),
            lookUpReferenceData.getJurisdiction2(),
            lookUpReferenceData.getKeyword(),
            lookUpReferenceData.getService()
        );
    }
}
