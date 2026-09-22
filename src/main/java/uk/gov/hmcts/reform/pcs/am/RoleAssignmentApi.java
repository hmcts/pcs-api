package uk.gov.hmcts.reform.pcs.am;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "role-assignment", url = "${role-assigment.url}")
public interface RoleAssignmentApi {
    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String AUTHORIZATION = "Authorization";

    @GetMapping(value = "/am/role-assignments/actors/{id}")
    RoleAssignmentResponse getRoles(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @RequestHeader(AUTHORIZATION) String authorisation,
        @PathVariable("id") String id
    );
}
