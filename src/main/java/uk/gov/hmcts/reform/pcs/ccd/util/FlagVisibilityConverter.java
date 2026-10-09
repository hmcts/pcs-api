package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlagVisibilityConverter {

    public static FlagVisibility toFlagVisibility(String visibility) {
        return FlagVisibility.EXTERNAL.getValue().equalsIgnoreCase(visibility)
            ? FlagVisibility.EXTERNAL
            : FlagVisibility.INTERNAL;
    }

}
