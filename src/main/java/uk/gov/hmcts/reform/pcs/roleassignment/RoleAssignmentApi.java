package uk.gov.hmcts.reform.pcs.roleassignment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.pcs.roleassignment.dto.CaseUserListDTO;

@FeignClient(name = "role-assignment", url = "${role-assignment.url}")
public interface RoleAssignmentApi {

    @PostMapping(
        value = "/case-users",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
     String assignRole(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("ServiceAuthorization") String serviceAuth,
        @RequestBody CaseUserListDTO request);
}
