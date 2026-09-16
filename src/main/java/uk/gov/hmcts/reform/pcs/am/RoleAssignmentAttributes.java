package uk.gov.hmcts.reform.pcs.am;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleAssignmentAttributes {

    String substantive;
    Long caseId;
    String jurisdiction;
    String caseType;
    String workTypes;
    String primaryLocation;

}
