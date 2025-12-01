package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.springframework.cloud.openfeign.FeignClient;
import uk.gov.hmcts.reform.fees.client.FeesApi;

@FeignClient(
    name = "fees-api-enforcement",
    url = "${fees.enforcement.url}",
    primary = false
)
public interface EnforcementFeesApi extends FeesApi {

}
