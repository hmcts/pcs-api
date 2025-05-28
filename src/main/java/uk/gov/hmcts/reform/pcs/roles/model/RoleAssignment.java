package uk.gov.hmcts.reform.pcs.roles.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.reform.pcs.roles.model.enums.ActorIdType;
import uk.gov.hmcts.reform.pcs.roles.model.enums.Classification;
import uk.gov.hmcts.reform.pcs.roles.model.enums.GrantType;
import uk.gov.hmcts.reform.pcs.roles.model.enums.RoleCategory;
import uk.gov.hmcts.reform.pcs.roles.model.enums.RoleType;

import java.time.Instant;
import java.util.List;

@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class RoleAssignment {

    private String id;
    private ActorIdType actorIdType;
    private String actorId;
    private RoleType roleType;
    private String roleName;
    private Classification classification;
    private GrantType grantType;
    private RoleCategory roleCategory;
    private Boolean readOnly;
    private Instant beginTime;
    private Instant endTime;
    private Instant created;
    private List<String> authorisations;
    private RoleAssignmentAttributes attributes;

}
