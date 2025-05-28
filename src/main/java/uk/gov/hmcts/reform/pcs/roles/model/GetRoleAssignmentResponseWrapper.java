package uk.gov.hmcts.reform.pcs.roles.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class GetRoleAssignmentResponseWrapper {

    @JsonProperty("roleAssignmentResponse")
    private List<RoleAssignment> roleAssignments;

}
