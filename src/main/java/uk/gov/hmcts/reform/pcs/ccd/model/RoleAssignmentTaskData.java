package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

@Data
@Builder
@AllArgsConstructor
public class RoleAssignmentTaskData {

    private final String caseReference;
    private final String userId;
    private final UserRole role;

}
