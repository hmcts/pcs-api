package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;

public class FlagVisibilityConverter {

    public static FlagVisibility toFlagVisibility(String visibility) {
        return FlagVisibility.EXTERNAL.getValue().equalsIgnoreCase(visibility)
            ? FlagVisibility.EXTERNAL
            : FlagVisibility.INTERNAL;
    }

}
