package uk.gov.hmcts.reform.pcs.payment.fee.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.payment.fee.api.FeesRegisterApi;
import uk.gov.hmcts.reform.pcs.payment.fee.config.FeesConfiguration;
import uk.gov.hmcts.reform.pcs.payment.fee.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.fee.model.FeeResponse;

@Service
@Slf4j
public class FeeService {

    private final AuthTokenGenerator authTokenGenerator;
    private final FeesConfiguration feesConfiguration;
    private final FeesRegisterApi feesRegisterApi;

    public FeeService(
        AuthTokenGenerator authTokenGenerator,
        FeesConfiguration feesConfiguration,
        FeesRegisterApi feesRegisterApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
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

        String serviceAuthorization = authTokenGenerator.generate();

        return feesRegisterApi.findFee(
                serviceAuthorization,
                lookUpReferenceData.getChannel(),
                lookUpReferenceData.getEvent(),
                lookUpReferenceData.getJurisdiction1(),
                lookUpReferenceData.getJurisdiction2(),
                lookUpReferenceData.getKeyword(),
                lookUpReferenceData.getService()
        );
    }
}
