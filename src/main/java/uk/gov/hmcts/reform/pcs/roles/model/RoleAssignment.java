package uk.gov.hmcts.reform.pcs.roles.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleAssignment {
    private String id;
    private String actorIdType; // currently IDAM
    private String actorId;
    private String roleType; // ORGANISATION, CASE
    private String roleName;
    private String classification;
    private String grantType; // BASIC, STANDARD, SPECIFIC, CHALLENGED, EXCLUDED
    private String roleCategory; // JUDICIAL, STAFF
    private Boolean readOnly;
    private Instant beginTime;
    private Instant endTime;
    private Instant created;
    private List<String> authorisations;
    private RoleAssignmentAttributes attributes;
}
