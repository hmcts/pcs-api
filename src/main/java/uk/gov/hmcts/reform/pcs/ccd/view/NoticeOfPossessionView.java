package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Optional;

@Component
public class NoticeOfPossessionView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getNoticeOfPossession)
            .ifPresent(noticeOfPossession ->
                           setNoticeOfPossessionFields(pcsCase, noticeOfPossession, pcsCaseEntity));
    }

    private void setNoticeOfPossessionFields(PCSCase pcsCase, NoticeOfPossessionEntity noticeOfPossessionEntity,
                                             PcsCaseEntity pcsCaseEntity) {
        NoticeServedDetails noticeServedDetails = new NoticeServedDetails();

        NoticeServiceMethod servingMethod = noticeOfPossessionEntity.getServingMethod();
        noticeServedDetails.setServiceMethod(servingMethod);
        setAbletoUploadDocument(noticeServedDetails, noticeOfPossessionEntity);
        noticeServedDetails.setUnableToUploadReason(noticeOfPossessionEntity.getUnableToUploadReason());

        List<ListValue<Document>> documents = getNoticeStatement(pcsCaseEntity);
        noticeServedDetails.setDocuments(documents);

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

    private static List<ListValue<Document>> getNoticeStatement(PcsCaseEntity pcsCaseEntity) {
        if (CollectionUtils.isEmpty(pcsCaseEntity.getDocuments())) {
            return List.of();
        }

        return pcsCaseEntity.getDocuments().stream()
            .filter(NoticeOfPossessionView::isNoticeStatement)
            .filter(DocumentsView::isDescriptionEmpty)
            .map(NoticeOfPossessionView::toDocument)
            .toList();
    }

    private static boolean isNoticeStatement(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.POSSESSION_NOTICE;
    }

    private static ListValue<Document> toDocument(DocumentEntity documentEntity) {
        return ListValue.<Document>builder()
            .id(documentEntity.getId().toString())
            .value(
                Document.builder()
                    .url(documentEntity.getUrl())
                    .filename(documentEntity.getFileName())
                    .binaryUrl(documentEntity.getBinaryUrl())
                    .categoryId(documentEntity.getCategoryId())
                    .build()
            ).build();
    }

    private static void setAbletoUploadDocument(NoticeServedDetails noticeServedDetails,
                                                NoticeOfPossessionEntity noticeOfPossessionEntity) {
        if (noticeOfPossessionEntity.getIsAbleToUploadDocument() != null) {
            noticeServedDetails.setAbleToUploadDocument(noticeOfPossessionEntity.getIsAbleToUploadDocument()
                    .equals(YesOrNo.YES) ? CanUploadNoticeServedDocument.Yes : CanUploadNoticeServedDocument.No);
        }
    }
}
