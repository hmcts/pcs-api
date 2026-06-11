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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.Email;
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
    private NoticeServiceMethod serviceMethod;

    // Date fields for different service methods
    @CCD(
            label = "Date the document was posted",
            hint = "For example, 16 4 2021",
            access = {CitizenAccess.class}
    )
    private LocalDate postedDate;

    @CCD(
            label = "Date the document was delivered",
            hint = "For example, 16 4 2021",
            access = {CitizenAccess.class}
    )
    private LocalDate deliveredDate;

    @CCD(
            label = "Date and time the document was handed over",
            hint = "For example, 16 4 2021, 11 15",
            access = {CitizenAccess.class}
    )
    private LocalDateTime handedOverDateTime;

    @CCD(
            label = "Date and time the document was emailed",
            hint = "For example, 16 4 2021, 11 15",
            access = {CitizenAccess.class}
    )
    private LocalDateTime emailSentDateTime;

    @CCD(
            label = "Date and time the message was sent",
            hint = "For example, 16 4 2021, 11 15",
            access = {CitizenAccess.class}
    )
    private LocalDateTime otherElectronicDateTime;

    @CCD(
            label = "Date and time the document was served",
            hint = "For example, 16 4 2021, 11 15",
            access = {CitizenAccess.class}
    )
    private LocalDateTime otherDateTime;

    // Text fields for different service methods
    @CCD(
            label = "Name of person the document was left with")
    private String personName;

    @CCD(
            label = "What email address was the document sent to?",
            hint = "For example, name@example.com",
            typeOverride = Email
    )
    private String emailAddress;

    @CCD(
            hint = "Give details of how the notice was served. You can enter up to 250 characters",
            typeOverride = TextArea
    )
    private String otherExplanation;

    @CCD(
            label = "Give details of how the notice was served",
            hint = "You can enter up to 250 characters",
            typeOverride = TextArea
    )
    private String otherElectronicExplanation;

    @CCD(
            label = "Are you able to upload a copy of the notice you served?",
            hint = "If you’ve served multiple notices, only upload one here. You can upload the other "
            + "copies and explain why you served multiple notices later on when we ask about additional reasons for "
            + "possession and whether you have other documents to upload"
    )
    private CanUploadNoticeServedDocument ableToUploadDocument;

    @CCD(
            label = "Upload a copy of the notice served",
            hint = "Upload a document to the system",
            typeOverride = FieldType.Collection,
            typeParameterOverride = "Document",
            access = {CaseworkerReadAccess.class}
    )
    private List<ListValue<Document>> documents;

    @CCD(
            label = "Why can you not upload a copy of the notice you served?",
            typeOverride = TextArea
    )
    private String unableToUploadReason;
}
