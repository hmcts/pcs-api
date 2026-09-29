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

    private UUID id;
    private String actorIdType;
    private UUID actorId;
    private String roleType;
    private String roleName;
    private String classification;
    private String grantType;
    private String roleCategory;
    private boolean readOnly;
    private LocalDateTime beginTime;
    private LocalDateTime created;
    private RoleAssignmentAttributes attributes;

}
