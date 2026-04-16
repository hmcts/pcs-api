package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;

public class YesOrNoConverter {

    public static Boolean toBoolean(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

    public static Boolean toBoolean(SimpleYesNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

    public static YesOrNo toYesOrNo(Boolean value) {
        return value != null ? YesOrNo.from(value) : null;
    }

    public static SimpleYesNo toVerticalYesNo(YesOrNo yesOrNo) {
        return yesOrNo != null ? SimpleYesNo.from(yesOrNo.toBoolean()) : null;
    }

}
