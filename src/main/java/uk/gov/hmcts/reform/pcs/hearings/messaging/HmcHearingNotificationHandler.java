package uk.gov.hmcts.reform.pcs.hearings.messaging;

import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;

public interface HmcHearingNotificationHandler {

    void handle(HmcHearingResponse notification);
}
