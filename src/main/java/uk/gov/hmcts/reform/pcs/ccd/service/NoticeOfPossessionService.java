package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Service
public class NoticeOfPossessionService {

    public NoticeOfPossessionEntity createNoticeOfPossessionEntity(PCSCase pcsCase) {

        NoticeServedDetails noticeServedDetails = pcsCase.getNoticeServedDetails();
        if (noticeServedDetails == null) {
            return null;
        }

        NoticeServiceMethod noticeServiceMethod = noticeServedDetails.getNoticeServiceMethod();
        if (noticeServiceMethod == null) {
            return null;
        }

        NoticeOfPossessionEntity noticeOfPossessionEntity = new NoticeOfPossessionEntity();

        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            WalesNoticeDetails walesNoticeDetails = pcsCase.getWalesNoticeDetails();
            noticeOfPossessionEntity.setNoticeServed(walesNoticeDetails.getNoticeServed());
            noticeOfPossessionEntity.setNoticeType(walesNoticeDetails.getTypeOfNoticeServed());
        } else {
            noticeOfPossessionEntity.setNoticeServed(pcsCase.getNoticeServed());
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

}
