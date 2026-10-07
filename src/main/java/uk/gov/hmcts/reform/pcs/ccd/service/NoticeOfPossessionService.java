package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
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
        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            setWalesNoticeFields(pcsCase, noticeOfPossessionEntity);
        }

        if (noticeServed == YesOrNo.NO) {
            return noticeOfPossessionEntity;
        }

        NoticeServedDetails noticeServedDetails = pcsCase.getNoticeServedDetails();

        NoticeServiceMethod noticeServiceMethod = noticeServedDetails.getServiceMethod();

        noticeOfPossessionEntity.setServingMethod(noticeServiceMethod);
        noticeOfPossessionEntity.setIsAbleToUploadDocument(getIsAbleToUploadDocument(noticeServedDetails));
        if (getIsAbleToUploadDocument(noticeServedDetails) == YesOrNo.NO) {
            noticeOfPossessionEntity.setUnableToUploadReason(noticeServedDetails.getUnableToUploadReason());
        }

        switch (noticeServiceMethod) {
            case FIRST_CLASS_POST ->
                noticeOfPossessionEntity.setNoticeDate(noticeServedDetails.getPostedDate());

            case DELIVERED_PERMITTED_PLACE ->
                noticeOfPossessionEntity.setNoticeDate(noticeServedDetails.getDeliveredDate());

            case PERSONALLY_HANDED -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getHandedOverDateTime());
                noticeOfPossessionEntity.setNoticeDetails(noticeServedDetails.getPersonName());
            }
            case EMAIL -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getEmailSentDateTime());
                noticeOfPossessionEntity.setNoticeDetails(noticeServedDetails.getEmailAddress());
            }
            case OTHER_ELECTRONIC -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getOtherElectronicDateTime());
                noticeOfPossessionEntity
                    .setNoticeDetails(noticeServedDetails.getOtherElectronicExplanation());
            }
            case OTHER -> {
                noticeOfPossessionEntity.setNoticeDateTime(noticeServedDetails.getOtherDateTime());
                noticeOfPossessionEntity.setNoticeDetails(noticeServedDetails.getOtherExplanation());
            }
        }

        return noticeOfPossessionEntity;
    }

    private static void setWalesNoticeFields(PCSCase pcsCase, NoticeOfPossessionEntity noticeOfPossessionEntity) {
        WalesNoticeDetails walesNoticeDetails = pcsCase.getWalesNoticeDetails();

        noticeOfPossessionEntity.setNoticeType(walesNoticeDetails.getTypeOfNoticeServed());
        noticeOfPossessionEntity.setNoticeStatement(walesNoticeDetails.getNoticeStatement());
    }

    private static YesOrNo getNoticeServed(PCSCase pcsCase) {
        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            WalesNoticeDetails walesNoticeDetails = pcsCase.getWalesNoticeDetails();
            return walesNoticeDetails.getNoticeServed();
        } else {
            return pcsCase.getNoticeServed();
        }
    }

    private static YesOrNo getIsAbleToUploadDocument(NoticeServedDetails noticeServedDetails) {
        if (noticeServedDetails.getAbleToUploadDocument() == CanUploadNoticeServedDocument.Yes) {
            return YesOrNo.YES;
        } else {
            return YesOrNo.NO;
        }
    }
}
