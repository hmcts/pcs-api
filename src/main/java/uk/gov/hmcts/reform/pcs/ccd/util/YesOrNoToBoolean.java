package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

public class YesOrNoToBoolean {

    public static Boolean convert(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

    public static Boolean convert(VerticalYesNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }
}
