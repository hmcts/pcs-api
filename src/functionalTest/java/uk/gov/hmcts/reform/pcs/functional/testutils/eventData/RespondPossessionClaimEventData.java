package uk.gov.hmcts.reform.pcs.functional.testutils.eventData;

import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.reform.pcs.functional.testutils.EventDataUtil;

public class RespondPossessionClaimEventData extends EventDataUtil {

    public RespondPossessionClaimEventData(ObjectNode data) {
        super(data);
    }
}