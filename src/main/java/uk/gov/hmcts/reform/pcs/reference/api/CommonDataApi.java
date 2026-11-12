package uk.gov.hmcts.reform.pcs.reference.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.pcs.reference.dto.ListOfValuesResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(name = "common-data", url = "${common-data.api-url}")
public interface CommonDataApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @GetMapping("/refdata/commondata/lov/categories/{categoryId}")
    ListOfValuesResponse getListOfValues(
        @PathVariable("categoryId") String categoryId,
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestParam("serviceId") String serviceId
    );
}
