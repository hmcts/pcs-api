package uk.gov.hmcts.reform.pcs.ccd.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SendClaimPackTaskData {

    private long caseReference;

}
