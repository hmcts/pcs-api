package uk.gov.hmcts.reform.pcs.noticeofchange.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.pcs.noticeofchange.model.AcaRequest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi.SERVICE_AUTHORIZATION;

@FeignClient(name = "aac-client", url = "${aac.api.url}")
public interface AssignCaseAccessClient {
    @PostMapping(value = "/noc/check-noc-approval", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SubmittedCallbackResponse checkNocApproval(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestBody AcaRequest nocApiRequest
    );

    @PostMapping(value = "/noc/apply-decision", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    AboutToStartOrSubmitCallbackResponse applyNoticeOfChange(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestBody AcaRequest nocApiRequest
    );
}
