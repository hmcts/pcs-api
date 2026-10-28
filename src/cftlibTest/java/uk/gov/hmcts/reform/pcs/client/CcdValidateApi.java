package uk.gov.hmcts.reform.pcs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.model.MidEventCaseDataContent;

@FeignClient(
    name = "ccd-validate-api",
    primary = false,
    url = "${core_case_data.api.url}"
)
public interface CcdValidateApi {

    @PostMapping(
        path = {"/case-types/{caseTypeId}/validate"},
        headers = {"experimental=true"}
    )
    CaseDetails validate(@RequestHeader("Authorization") String authorisation,
                         @RequestHeader("ServiceAuthorization") String serviceAuthorisation,
                         @PathVariable("caseTypeId") String caseType,
                         @RequestParam("pageId") String pageId,
                         @RequestBody MidEventCaseDataContent caseDataContent);


}
