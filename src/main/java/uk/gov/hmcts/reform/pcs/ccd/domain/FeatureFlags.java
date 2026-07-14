package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeatureFlags {

    private VerticalYesNo release1dot2Enabled;
    private VerticalYesNo caseWorkerEventsEnabled;

}
