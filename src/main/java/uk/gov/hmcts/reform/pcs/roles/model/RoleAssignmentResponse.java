package uk.gov.hmcts.reform.pcs.roles.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
public class RoleAssignmentResponse {

    @JsonProperty("roleAssignmentResponse")
    private List<RoleAssignment> roleAssignments;

}
