package uk.gov.hmcts.reform.pcs.assigncaseaccess;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;

@FeignClient(name = "manage-case-assignment-api", url = "${role-assignment.url}")
public interface AssignCaseAccessApi {

    @PostMapping(
        value = "/case-users",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    CaseAssignmentUserRolesResponse assignRole(
        @RequestHeader("Authorization") String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody final CaseAssignmentUserRolesRequest assignCaseAccessRequest
    );
}