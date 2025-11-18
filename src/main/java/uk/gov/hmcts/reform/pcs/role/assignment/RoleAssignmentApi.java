package uk.gov.hmcts.reform.pcs.role.assignment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.pcs.role.assignment.DTO.CaseUserListDTO;

@FeignClient(name = "role-assignment", url = "${role-assignment.url}")
public interface RoleAssignmentApi {

    @PostMapping(
        value = "http://ccd-data-store-api-aat.service.core-compute-aat.internal/case-users",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String assignRole(
        @RequestHeader("ServiceAuthorization") String serviceAuth,
        @RequestBody CaseUserListDTO request);
}
