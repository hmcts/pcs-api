package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerReadAccess;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class NoticeServedDetails {

    // Notice Details fields
    @CCD(
            label = "How did you serve the notice?"
    )
    private NoticeServiceMethod noticeServiceMethod;

    // Date fields for different service methods
    @CCD(
            label = "Date the document was posted",
            hint = "For example, 16 4 2021"
    )
    private LocalDate noticePostedDate;

    @CCD(
            label = "Date the document was delivered",
            hint = "For example, 16 4 2021"
    )
    private LocalDate noticeDeliveredDate;

    @CCD(
            label = "Date and time the document was handed over",
            hint = "For example, 16 4 2021, 11 15"
    )
    private LocalDateTime noticeHandedOverDateTime;

    @CCD(
            label = "Date and time the email was sent",
            hint = "For example, 16 4 2021, 11 15"
    )
    private LocalDateTime noticeEmailSentDateTime;

    @CCD(
            label = "Date and time the electronic message was sent",
            hint = "For example, 16 4 2021, 11 15"
    )
    private LocalDateTime noticeOtherElectronicDateTime;

    @CCD(
            label = "Date and time the document was served",
            hint = "For example, 16 4 2021, 11 15"
    )
    private LocalDateTime noticeOtherDateTime;

    // Text fields for different service methods
    @CCD(
            label = "Name of person the document was left with",
            max = 60
    )
    private String noticePersonName;

    @CCD(
            label = "Explain how it was served by email",
            hint = "You can enter up to 250 characters",
            typeOverride = TextArea
    )
    private String noticeEmailExplanation;

    @CCD(
            label = "Explain what the other means were",
            hint = "You can enter up to 250 characters",
            typeOverride = TextArea
    )
    private String noticeOtherExplanation;

    @CCD(
            label = "Add document",
            hint = "Upload a document to the system",
            typeOverride = FieldType.Collection,
            typeParameterOverride = "Document",
            access = {CaseworkerReadAccess.class}
    )
    private List<ListValue<Document>> noticeDocuments;
}
