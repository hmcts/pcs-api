package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

public interface GenAppRequest {

    GenAppType getApplicationType();

    VerticalYesNo getWithin14Days();

    VerticalYesNo getNeedHwf();

    VerticalYesNo getAppliedForHwf();

    String getHwfReference();

    VerticalYesNo getOtherPartiesAgreed();

    VerticalYesNo getWithoutNotice();

    String getWithoutNoticeReason();

    String getWhatOrderWanted();

    LanguageUsed getLanguageUsed();

    VerticalYesNo getSotAccepted();

    String getSotFullName();

    default String getClientReference() {
        return null;
    }
}
