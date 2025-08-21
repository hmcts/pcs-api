package uk.gov.hmcts.reform.pcs.payment.fee.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.payment.fee.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.payment.fee.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.payment.fee.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.fee.model.FeeResponse;

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
        FeesConfiguration.LookUpReferenceData lookUpReferenceData = feesConfiguration.getFees().get("defaultFee");

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
