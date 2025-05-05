package uk.gov.hmcts.reform.pcs.roles.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.pcs.roles.model.RoleAssignmentResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;


@FeignClient(name = "role-assignment-service", url = "${role-assignment-service.api.url}")
public interface RoleAssignmentServiceApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String HEARING_ENDPOINT = "/am/role-assignments";
    String ID = "id";

    @GetMapping(value = HEARING_ENDPOINT + "/actors/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    RoleAssignmentResponse getRoleAssignments(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @PathVariable(ID) String actorId
    );

}
