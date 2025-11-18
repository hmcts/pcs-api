package uk.gov.hmcts.reform.pcs.role.assignment.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CaseUserListDTO {
    @JsonProperty("case_users")
    private List<CaseUserDTO> caseUsers;
}
