package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
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
public class NoticeOfPossessionView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getNoticeOfPossession)
            .ifPresent(noticeOfPossession -> setNoticeOfPossessionFields(pcsCase, noticeOfPossession));
    }

    private void setNoticeOfPossessionFields(PCSCase pcsCase, NoticeOfPossessionEntity noticeOfPossessionEntity) {
        NoticeServedDetails noticeServedDetails = new NoticeServedDetails();

        NoticeServiceMethod servingMethod = noticeOfPossessionEntity.getServingMethod();
        noticeServedDetails.setNoticeServiceMethod(servingMethod);

        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            WalesNoticeDetails walesNoticeDetails = WalesNoticeDetails.builder()
                .noticeServed(noticeOfPossessionEntity.getNoticeServed())
                .typeOfNoticeServed(noticeOfPossessionEntity.getNoticeType())
                .build();

            pcsCase.setWalesNoticeDetails(walesNoticeDetails);
        } else {
            pcsCase.setNoticeServed(noticeOfPossessionEntity.getNoticeServed());
        }

        switch (servingMethod) {
            case FIRST_CLASS_POST -> {
                noticeServedDetails.setNoticePostedDate(noticeOfPossessionEntity.getNoticeDate());
            }
            case DELIVERED_PERMITTED_PLACE -> {
                noticeServedDetails.setNoticeDeliveredDate(noticeOfPossessionEntity.getNoticeDate());
            }
            case PERSONALLY_HANDED -> {
                noticeServedDetails.setNoticeHandedOverDateTime(noticeOfPossessionEntity.getNoticeDateTime());
                noticeServedDetails.setNoticePersonName(noticeOfPossessionEntity.getNoticeDetails());
            }
            case EMAIL -> {
                noticeServedDetails.setNoticeEmailSentDateTime(noticeOfPossessionEntity.getNoticeDateTime());
                noticeServedDetails.setNoticeEmailExplanation(noticeOfPossessionEntity.getNoticeDetails());
            }
            case OTHER_ELECTRONIC -> {
                noticeServedDetails.setNoticeOtherElectronicDateTime(noticeOfPossessionEntity.getNoticeDateTime());
            }
            case OTHER -> {
                noticeServedDetails.setNoticeOtherDateTime(noticeOfPossessionEntity.getNoticeDateTime());
                noticeServedDetails.setNoticeOtherExplanation(noticeOfPossessionEntity.getNoticeDetails());
            }
        }
        pcsCase.setNoticeServedDetails(noticeServedDetails);
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

}
