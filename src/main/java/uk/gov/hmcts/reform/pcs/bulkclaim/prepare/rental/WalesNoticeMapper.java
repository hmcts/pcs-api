package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NoticeType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.WalesNoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;

/**
 * Maps the notice of possession for a property in Wales: whether it was served, its type and how it was served,
 * or the statement explaining why it was not served.
 */
@Component
public class WalesNoticeMapper {

    private static final int NOTICE_TYPE_LIMIT = 10;
    private static final String NOTICE_TYPE_LABEL = "Type of notice served";

    private final RentalNoticeMapper noticeMapper;

    public WalesNoticeMapper(RentalNoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    /**
     * Maps the Welsh notice of possession onto the case.
     *
     * @param path path of the WalesNotice element relative to the claim
     * @return the notice type for the claim extras, or null
     */
    public String apply(WalesNoticeType notice, PCSCase target, String path, BulkErrorCollector errors) {
        if (notice == null) {
            errors.add(path, BulkErrorCollector.REQUIRED_CODE, "WalesNotice is required for a claim in Wales");
            return null;
        }

        if (notice.isServed()) {
            errors.requiredWithMaxLength(path + "/TypeOfNoticeServed", notice.getTypeOfNoticeServed(),
                                         NOTICE_TYPE_LIMIT, NOTICE_TYPE_LABEL);
            target.setWalesNoticeDetails(WalesNoticeDetails.builder()
                .noticeServed(YesOrNo.YES)
                .typeOfNoticeServed(notice.getTypeOfNoticeServed())
                .build());
            target.setNoticeServedDetails(noticeMapper.servedDetails(servedNotice(notice), path, errors));
            return notice.getTypeOfNoticeServed();
        }

        errors.requiredWithMaxLength(path + "/NoticeStatement", notice.getNoticeStatement(),
                                     BulkLimits.MEDIUM_TEXT, WalesNoticeDetails.NOTICE_STATEMENT_LABEL);
        target.setWalesNoticeDetails(WalesNoticeDetails.builder()
            .noticeServed(YesOrNo.NO)
            .noticeStatement(notice.getNoticeStatement())
            .build());
        return null;
    }

    private static NoticeType servedNotice(WalesNoticeType notice) {
        NoticeType served = new NoticeType();
        served.setServed(true);
        served.setServiceMethod(notice.getServiceMethod());
        served.setServedDate(notice.getServedDate());
        served.setServedDateTime(notice.getServedDateTime());
        served.setPersonName(notice.getPersonName());
        served.setEmailAddress(notice.getEmailAddress());
        served.setExplanation(notice.getExplanation());
        return served;
    }
}
