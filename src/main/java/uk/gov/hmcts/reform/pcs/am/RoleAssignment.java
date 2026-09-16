package uk.gov.hmcts.reform.pcs.am;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleAssignment {

    UUID id;
    String actorIdType;
    UUID actorId;
    String roleType;
    String roleName;
    String classification;
    String grantType;
    String roleCategory;
    boolean readOnly;
    LocalDateTime beginTime;
    LocalDateTime created;
    RoleAssignmentAttributes attributes;

}
