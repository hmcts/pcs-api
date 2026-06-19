package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DeleteDraftClaimTaskData {

    private final String caseReference;
}
