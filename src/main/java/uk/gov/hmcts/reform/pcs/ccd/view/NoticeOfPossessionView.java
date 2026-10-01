package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NoticeOfPossessionView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getNoticeOfPossession)
            .ifPresent(noticeOfPossession ->
                           setNoticeOfPossessionFields(pcsCase, noticeOfPossession));
    }

    private void setNoticeOfPossessionFields(PCSCase pcsCase, NoticeOfPossessionEntity noticeOfPossessionEntity) {
        NoticeServedDetails noticeServedDetails = new NoticeServedDetails();

        NoticeServiceMethod servingMethod = noticeOfPossessionEntity.getServingMethod();
        noticeServedDetails.setServiceMethod(servingMethod);
        setAbletoUploadDocument(noticeServedDetails, noticeOfPossessionEntity);
        noticeServedDetails.setUnableToUploadReason(noticeOfPossessionEntity.getUnableToUploadReason());

        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            WalesNoticeDetails walesNoticeDetails = WalesNoticeDetails.builder()
                .noticeServed(noticeOfPossessionEntity.getNoticeServed())
                .typeOfNoticeServed(noticeOfPossessionEntity.getNoticeType())
                .noticeStatement(noticeOfPossessionEntity.getNoticeStatement())
                .build();

            pcsCase.setWalesNoticeDetails(walesNoticeDetails);
        } else {
            pcsCase.setNoticeServed(noticeOfPossessionEntity.getNoticeServed());
        }

        if (servingMethod != null) {
            switch (servingMethod) {
                case FIRST_CLASS_POST -> {
                    noticeServedDetails.setPostedDate(noticeOfPossessionEntity.getNoticeDate());
                }
                case DELIVERED_PERMITTED_PLACE -> {
                    noticeServedDetails.setDeliveredDate(noticeOfPossessionEntity.getNoticeDate());
                }
                case PERSONALLY_HANDED -> {
                    noticeServedDetails.setHandedOverDateTime(noticeOfPossessionEntity.getNoticeDateTime());
                    noticeServedDetails.setPersonName(noticeOfPossessionEntity.getNoticeDetails());
                }
                case EMAIL -> {
                    noticeServedDetails.setEmailSentDateTime(noticeOfPossessionEntity.getNoticeDateTime());
                    noticeServedDetails.setEmailAddress(noticeOfPossessionEntity.getNoticeDetails());
                }
                case OTHER_ELECTRONIC -> {
                    noticeServedDetails.setOtherElectronicDateTime(noticeOfPossessionEntity.getNoticeDateTime());
                    noticeServedDetails.setOtherElectronicExplanation(noticeOfPossessionEntity.getNoticeDetails());
                }
                case OTHER -> {
                    noticeServedDetails.setOtherDateTime(noticeOfPossessionEntity.getNoticeDateTime());
                    noticeServedDetails.setOtherExplanation(noticeOfPossessionEntity.getNoticeDetails());
                }
            }
        }

        pcsCase.setNoticeServedDetails(noticeServedDetails);
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

    private static void setAbletoUploadDocument(NoticeServedDetails noticeServedDetails,
                                                NoticeOfPossessionEntity noticeOfPossessionEntity) {
        if (noticeOfPossessionEntity.getIsAbleToUploadDocument() != null) {
            noticeServedDetails.setAbleToUploadDocument(noticeOfPossessionEntity.getIsAbleToUploadDocument()
                    .equals(YesOrNo.YES) ? CanUploadNoticeServedDocument.Yes : CanUploadNoticeServedDocument.No);
        }
    }
}
