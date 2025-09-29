package uk.gov.hmcts.reform.pcs.ccd.utils;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

public class YesOrNoToBoolean {

    public static Boolean convert(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }
}
