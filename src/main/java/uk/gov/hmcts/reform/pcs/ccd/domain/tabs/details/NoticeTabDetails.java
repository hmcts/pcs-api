package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeTabDetails {

    @CCD(label = "Has notice been served?")
    private String noticeServed;

    @CCD(label = "Type of notice served")
    private String typeOfNoticeServed;

    @CCD(label = "Statement")
    private String statement;

    @CCD(label = "Date and time notice served (if applicable)")
    private String noticeDate;

    @CCD(label = "Method of service")
    private String noticeMethod;

    @CCD(label = "Name of person document was left with")
    private String noticePersonName;

    @CCD(label = "Email address the document sent to")
    private String noticeEmailAddress;

    @CCD(label = "Details of how notice was served")
    private String noticeOtherElectronicDetails;

    @CCD(label = "Explain what the other means were")
    private String noticeOtherExplanation;

    @CCD(label = "Are you able to upload a copy of the notice you served?")
    private String noticeUploaded;

    @CCD(label = "Notice or certificate of service")
    private List<ListValue<Document>> noticeDocuments;

    @CCD(label = "Details of why you cannot upload a copy")
    private String reasonsForNoNoticeDocument;
}
