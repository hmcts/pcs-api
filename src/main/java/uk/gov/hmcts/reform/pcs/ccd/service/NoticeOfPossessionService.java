package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Service
public class NoticeOfPossessionService {

    public NoticeOfPossessionEntity createNoticeOfPossessionEntity(PCSCase pcsCase) {

        NoticeOfPossessionEntity noticeOfPossessionEntity = new NoticeOfPossessionEntity();

        YesOrNo noticeServed = getNoticeServed(pcsCase);
        noticeOfPossessionEntity.setNoticeServed(noticeServed);

        if (noticeServed == YesOrNo.NO) {
            return noticeOfPossessionEntity;
        }

        NoticeServedDetails noticeServedDetails = pcsCase.getNoticeServedDetails();
        if (noticeServedDetails == null) {
            return noticeOfPossessionEntity;
        }

        NoticeServiceMethod noticeServiceMethod = noticeServedDetails.getNoticeServiceMethod();
        if (noticeServiceMethod == null) {
            return noticeOfPossessionEntity;
        }

        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            WalesNoticeDetails walesNoticeDetails = pcsCase.getWalesNoticeDetails();
            noticeOfPossessionEntity.setNoticeType(walesNoticeDetails.getTypeOfNoticeServed());
        }

        noticeOfPossessionEntity.setServingMethod(noticeServiceMethod);

        switch (noticeServiceMethod) {
            case FIRST_CLASS_POST -> {
                noticeOfPossessionEntity.setNoticeDate(noticeServedDetails.getNoticePostedDate());
            }
            case DELIVERED_PERMITTED_PLACE -> {
                noticeOfPossessionEntity.setNoticeDate(noticeServedDetails.getNoticeDeliveredDate());
            }
            case PERSONALLY_HANDED -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getNoticeHandedOverDateTime());
                noticeOfPossessionEntity.setNoticeDetails(noticeServedDetails.getNoticePersonName());
            }
            case EMAIL -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getNoticeEmailSentDateTime());
                noticeOfPossessionEntity.setNoticeDetails(noticeServedDetails.getNoticeEmailExplanation());
            }
            case OTHER_ELECTRONIC -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getNoticeOtherElectronicDateTime());
            }
            case OTHER -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getNoticeOtherDateTime());
                noticeOfPossessionEntity.setNoticeDetails(noticeServedDetails.getNoticeOtherExplanation());
            }
        }

        return noticeOfPossessionEntity;
    }

    private static YesOrNo getNoticeServed(PCSCase pcsCase) {
        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            WalesNoticeDetails walesNoticeDetails = pcsCase.getWalesNoticeDetails();
            return walesNoticeDetails.getNoticeServed();
        } else {
            return pcsCase.getNoticeServed();
        }
    }

}
