package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

public class YesOrNoConverter {

    public static Boolean toBoolean(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

    public static Boolean toBoolean(VerticalYesNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

    public static VerticalYesNo toVerticalYesNo(YesOrNo yesOrNo) {
        return yesOrNo != null ? VerticalYesNo.from(yesOrNo.toBoolean()) : null;
    }

}
