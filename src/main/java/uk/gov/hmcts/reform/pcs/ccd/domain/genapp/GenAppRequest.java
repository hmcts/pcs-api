package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

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

    VerticalYesNo getHasSupportingDocuments();

    List<ListValue<UploadedDocument>> getUploadedDocuments();

    LanguageUsed getLanguageUsed();

    VerticalYesNo getSotAccepted();

    String getSotFullName();

    default String getClientReference() {
        return null;
    }
}
