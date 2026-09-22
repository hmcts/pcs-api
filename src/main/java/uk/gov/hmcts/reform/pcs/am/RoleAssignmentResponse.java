package uk.gov.hmcts.reform.pcs.am;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleAssignmentResponse {

    @JsonProperty("roleAssignmentResponse")
    List<RoleAssignment> roleAssignment;

}
