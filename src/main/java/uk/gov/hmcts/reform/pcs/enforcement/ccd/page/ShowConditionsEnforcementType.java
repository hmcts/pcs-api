package uk.gov.hmcts.reform.pcs.enforcement.ccd.page;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.or;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShowConditionsEnforcementType {

    public static final String WARRANT_FLOW = "chooseEnforcementType=\"WARRANT\"";
    public static final String WRIT_FLOW = "chooseEnforcementType=\"WRIT\"";
    public static final String WARRANT_OF_RESTITUTION_FLOW = "chooseEnforcementType=\"WARRANT_OF_RESTITUTION\"";
    public static final String WRIT_OF_RESTITUTION_FLOW = "chooseEnforcementType=\"WRIT_OF_RESTITUTION\"";

    /**
     * The warrant journeys: a warrant of restitution asks the same language and statement of truth questions as
     * a warrant of possession, on the same pages (HDPI-7555).
     */
    public static final String WARRANT_OR_RESTITUTION_FLOW = or(WARRANT_FLOW, WARRANT_OF_RESTITUTION_FLOW);

    /**
     * The writ journeys, which share the writ language and statement of truth pages the same way.
     */
    public static final String WRIT_OR_RESTITUTION_FLOW = or(WRIT_FLOW, WRIT_OF_RESTITUTION_FLOW);

}
