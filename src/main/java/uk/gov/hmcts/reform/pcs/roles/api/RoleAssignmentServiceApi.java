package uk.gov.hmcts.reform.pcs.roles.api;

import org.springframework.cloud.openfeign.FeignClient;


@FeignClient(name = "role-assignment-service", url = "${role-assignment-service.api.url}")
public interface RoleAssignmentServiceApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String ROLE_ASSIGNMENTS_ENDPOINT = "/am/role-assignments";
    String ID = "id";

//    @GetMapping(value = ROLE_ASSIGNMENTS_ENDPOINT + "/actors/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
//    GetRoleAssignmentResponseWrapper getRoleAssignments(
//        @RequestHeader(AUTHORIZATION) String authorisation,
//        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
//        @PathVariable(ID) String actorId
//    );
//
//    @PostMapping(value = ROLE_ASSIGNMENTS_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE)
//    CreateRoleAssignmentResponseWrapper createRoleAssignments(
//        @RequestHeader(AUTHORIZATION) String authorisation,
//        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
//        @RequestBody RoleAssignmentRequest roleAssignmentRequest
//    );

}
