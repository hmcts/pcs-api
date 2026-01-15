package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel. PRIVATE)
public final class ShowConditionsWarrantOrWrit {

    public static final String WARRANT_FLOW = "selectEnforcementType=\"WARRANT\"";
    public static final String WRIT_FLOW = "selectEnforcementType=\"WRIT\"";

}
