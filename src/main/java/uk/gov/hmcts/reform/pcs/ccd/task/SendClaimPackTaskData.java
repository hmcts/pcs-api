package uk.gov.hmcts.reform.pcs.ccd.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class SendClaimPackTaskData {

    private long caseReference;

}
