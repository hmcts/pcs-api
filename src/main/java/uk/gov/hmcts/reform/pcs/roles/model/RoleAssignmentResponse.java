package uk.gov.hmcts.reform.pcs.roles.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
public class RoleAssignmentResponse {

    private RoleRequest roleRequest;

    private Collection<RoleAssignment> requestedRoles;

}
