package uk.gov.hmcts.reform.pcs.noc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CaseUserRolesRequest(
    @JsonProperty("case_users") List<CaseUserRoleWithOrganisation> caseUsers
) {
}
