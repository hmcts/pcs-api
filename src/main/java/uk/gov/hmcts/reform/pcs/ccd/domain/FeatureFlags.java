package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlags {

    private VerticalYesNo release1dot2Enabled;
    private VerticalYesNo release1dot3Enabled;
    private VerticalYesNo caseWorkerEventsEnabled;
    private VerticalYesNo walesMakeAClaimEnabled;
    private VerticalYesNo cuiRespondToClaimLrEnabled;

}
