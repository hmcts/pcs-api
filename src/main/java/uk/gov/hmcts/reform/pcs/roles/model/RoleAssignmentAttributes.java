package uk.gov.hmcts.reform.pcs.roles.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleAssignmentAttributes {

    private String jurisdiction;
    private String caseId;
    private String caseType;
    private String region;
    private String location;
    private String contractType;
    private String caseAccessGroupId;
    private String substantive;

}
