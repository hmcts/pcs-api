package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Payload for the hearing-notice-generation db-scheduler task.
 */
@Data
@Builder
@AllArgsConstructor
public class HearingNoticeTaskData {

    private final long caseReference;
    private final Integer hearingId;

}
