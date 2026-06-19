package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DeleteDraftClaimRoleRevocationTaskData {

    private final String caseReference;
    private final String userId;
}
