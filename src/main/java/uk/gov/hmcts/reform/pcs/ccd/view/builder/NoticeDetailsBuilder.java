package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.view.DocumentsView;
import uk.gov.hmcts.reform.pcs.ccd.view.UploadTimestampProvider;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.view.CaseDetailsTabUtil.DATE_FORMATTER;
import static uk.gov.hmcts.reform.pcs.ccd.view.CaseDetailsTabUtil.NO_ANSWER;
import static uk.gov.hmcts.reform.pcs.ccd.view.CaseDetailsTabUtil.formatDateTime;

@Component
@RequiredArgsConstructor
public class NoticeDetailsBuilder {

    private final UploadTimestampProvider uploadTimestampProvider;
    
    public NoticeTabDetails buildNoticeTabDetails(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            return buildNoticeTabDetailsWales(pcsCase, pcsCaseEntity);
        }

        return buildNoticeTabDetailsEngland(pcsCase, pcsCaseEntity);
    }

    private NoticeTabDetails buildNoticeTabDetailsEngland(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (pcsCase.getNoticeServed() == null) {
            return NoticeTabDetails.builder()
                    .noticeServed(NO_ANSWER)
                    .noticeMethod(NO_ANSWER)
                    .noticeDate(NO_ANSWER)
                    .build();
        }

        YesOrNo noticeServed = pcsCase.getNoticeServed();
        NoticeTabDetails noticeTabDetails = NoticeTabDetails.builder()
                .noticeServed(noticeServed.getValue())
                .noticeMethod(NO_ANSWER)
                .noticeDate(NO_ANSWER)
                .build();

        if (noticeServed == YesOrNo.YES) {
            populateNoticeDetails(noticeTabDetails, pcsCase.getNoticeServedDetails(), pcsCaseEntity);
        }

        return noticeTabDetails;
    }

    private NoticeTabDetails buildNoticeTabDetailsWales(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        WalesNoticeDetails walesNoticeDetails = pcsCase.getWalesNoticeDetails();

        if (walesNoticeDetails == null) {
            return NoticeTabDetails.builder()
                    .noticeServed(NO_ANSWER)
                    .noticeMethod(NO_ANSWER)
                    .noticeDate(NO_ANSWER)
                    .build();
        }

        YesOrNo noticeServed = walesNoticeDetails.getNoticeServed();

        NoticeTabDetails noticeTabDetails = NoticeTabDetails.builder()
                .noticeServed(noticeServed != null ? noticeServed.getValue() : NO_ANSWER)
                .typeOfNoticeServed(noticeServed == YesOrNo.YES ? walesNoticeDetails.getTypeOfNoticeServed() : null)
                .statement(noticeServed == YesOrNo.NO ? walesNoticeDetails.getNoticeStatement() : null)
                .noticeMethod(NO_ANSWER)
                .noticeDate(NO_ANSWER)
                .build();

        populateNoticeDetails(noticeTabDetails, pcsCase.getNoticeServedDetails(), pcsCaseEntity);

        return noticeTabDetails;
    }

    private void populateNoticeDetails(
        NoticeTabDetails noticeTabDetails,
        NoticeServedDetails noticeServedDetails,
        PcsCaseEntity pcsCaseEntity
    ) {
        if (noticeServedDetails == null || noticeServedDetails.getServiceMethod() == null) {
            return;
        }

        List<ListValue<Document>> documents = getNoticeStatement(pcsCaseEntity);
        noticeTabDetails.setNoticeDocuments(documents);
        noticeTabDetails.setNoticeUploaded(String.valueOf(noticeServedDetails.getAbleToUploadDocument()));
        noticeTabDetails.setReasonsForNoNoticeDocument(noticeServedDetails.getUnableToUploadReason());

        NoticeServiceMethod method = noticeServedDetails.getServiceMethod();
        noticeTabDetails.setNoticeMethod(method.getLabel());
        applyNoticeMethodDetails(noticeTabDetails, method, noticeServedDetails);

    }

    private void applyNoticeMethodDetails(NoticeTabDetails noticeTabDetails, NoticeServiceMethod method,
                                          NoticeServedDetails noticeServedDetails) {
        switch (method) {
            case FIRST_CLASS_POST -> handleFirstClassPost(noticeTabDetails, noticeServedDetails);
            case DELIVERED_PERMITTED_PLACE -> handleDeliveredPermittedPlace(noticeTabDetails, noticeServedDetails);
            case PERSONALLY_HANDED -> handlePersonallyHanded(noticeTabDetails, noticeServedDetails);
            case EMAIL -> handleEmail(noticeTabDetails, noticeServedDetails);
            case OTHER_ELECTRONIC -> handleOtherElectronic(noticeTabDetails, noticeServedDetails);
            case OTHER -> handleOther(noticeTabDetails, noticeServedDetails);
        }
    }

    private void handleFirstClassPost(NoticeTabDetails noticeTabDetails, NoticeServedDetails noticeServedDetails) {
        LocalDate date = noticeServedDetails.getPostedDate();
        noticeTabDetails.setNoticeDate(date != null ? date.format(DATE_FORMATTER) : NO_ANSWER);
    }

    private void handleDeliveredPermittedPlace(NoticeTabDetails noticeTabDetails,
                                               NoticeServedDetails noticeServedDetails) {
        LocalDate date = noticeServedDetails.getDeliveredDate();
        noticeTabDetails.setNoticeDate(date != null ? date.format(DATE_FORMATTER) : NO_ANSWER);
    }

    private void handlePersonallyHanded(NoticeTabDetails noticeTabDetails, NoticeServedDetails noticeServedDetails) {
        LocalDateTime dateTime = noticeServedDetails.getHandedOverDateTime();
        String name = noticeServedDetails.getPersonName();
        noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
        noticeTabDetails.setNoticePersonName(name != null ? name : NO_ANSWER);
    }

    private void handleEmail(NoticeTabDetails noticeTabDetails, NoticeServedDetails noticeServedDetails) {
        LocalDateTime dateTime = noticeServedDetails.getEmailSentDateTime();
        String emailAddress = noticeServedDetails.getEmailAddress();
        noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
        noticeTabDetails.setNoticeEmailAddress(emailAddress != null ? emailAddress : NO_ANSWER);
    }

    private void handleOtherElectronic(NoticeTabDetails noticeTabDetails, NoticeServedDetails noticeServedDetails) {
        LocalDateTime dateTime = noticeServedDetails.getOtherElectronicDateTime();
        String details = noticeServedDetails.getOtherElectronicExplanation();
        noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
        noticeTabDetails.setNoticeOtherElectronicDetails(details != null ? details : NO_ANSWER);
    }

    private void handleOther(NoticeTabDetails noticeTabDetails, NoticeServedDetails noticeServedDetails) {
        LocalDateTime dateTime = noticeServedDetails.getOtherDateTime();
        String explanation = noticeServedDetails.getOtherExplanation();
        noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
        noticeTabDetails.setNoticeOtherExplanation(explanation != null ? explanation : NO_ANSWER);
    }

    private List<ListValue<Document>> getNoticeStatement(PcsCaseEntity pcsCaseEntity) {
        if (CollectionUtils.isEmpty(pcsCaseEntity.getDocuments())) {
            return List.of();
        }

        return pcsCaseEntity.getDocuments().stream()
            .filter(NoticeDetailsBuilder::isNoticeStatement)
            .filter(DocumentsView::isNotGenAppDocument)
            .filter(DocumentsView::isDescriptionEmpty)
            .filter(DocumentsView::isNotRemoved)
            .map(this::toDocument)
            .toList();
    }

    private static boolean isNoticeStatement(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.POSSESSION_NOTICE;
    }

    private ListValue<Document> toDocument(DocumentEntity documentEntity) {
        return ListValue.<Document>builder()
            .id(documentEntity.getId().toString())
            .value(
                Document.builder()
                    .url(documentEntity.getUrl())
                    .filename(documentEntity.getFileName())
                    .binaryUrl(documentEntity.getBinaryUrl())
                    .categoryId(documentEntity.getCategoryId())
                    .uploadTimestamp(uploadTimestampProvider.uploadTimestamp(documentEntity))
                    .build()
            ).build();
    }
}
