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

    private String substantive;
    private Long caseId;
    private String jurisdiction;
    private String caseType;
    private String workTypes;
    private String primaryLocation;
    /**
     * Set on group-access role assignments only: the case access group the role applies to, which a case
     * matches when its CaseAccessGroups hold the same id.
     */
    private String caseAccessGroupId;

}
