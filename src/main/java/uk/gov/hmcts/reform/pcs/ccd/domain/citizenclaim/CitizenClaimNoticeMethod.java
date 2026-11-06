package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;

@AllArgsConstructor
@Getter
public enum CitizenClaimNoticeMethod implements HasLabel {

    FIRST_CLASS_POST("By first class post or a different service which provides for delivery on the next "
                         + "business day", NoticeServiceMethod.FIRST_CLASS_POST),
    PERMITTED_PLACE("By delivering to or leaving it at a permitted place",
                    NoticeServiceMethod.DELIVERED_PERMITTED_PLACE),
    PERSONALLY_HANDED("By personally handing it to or leaving it with someone",
                      NoticeServiceMethod.PERSONALLY_HANDED),
    EMAIL("By email", NoticeServiceMethod.EMAIL),
    OTHER_ELECTRONIC("By other electronic method", NoticeServiceMethod.OTHER_ELECTRONIC),
    OTHER("Other", NoticeServiceMethod.OTHER);

    private final String label;
    private final NoticeServiceMethod noticeServiceMethod;
}
