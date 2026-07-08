package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDate;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EnterGenAppRequest {

    @CCD(
        label = "Which party made the application?",
        typeOverride = FieldType.DynamicRadioList
    )
    private DynamicList applicantParty;

    @CCD(label = "What date was the application received?")
    private LocalDate dateReceived;

    @CCD(label = "Which type of application has the applicant made?")
    private EnterGenAppType applicationTypeOption;

    @CCD(label = "Is there a hearing for this case in the next 14 days?")
    private VerticalYesNo within14Days;

    @CCD(label = "Have they already applied for help with their application fee?")
    private VerticalYesNo appliedForHwf;

    @CCD(
        label = "Enter their Help with Fees reference number",
        max = 60
    )
    private String hwfReference;

    @CCD(label = "Have the other parties agreed to this application?")
    private VerticalYesNo otherPartiesAgreed;

    @CCD(label = "Are there any reasons that this application should not be shared with the other parties?")
    private VerticalYesNo withoutNotice;

    @CCD(label = "Add document")
    private List<ListValue<UploadedDocument>> uploadedDocuments;

    @CCD(
        label = "Tell us which categories apply",
        typeOverride = FieldType.TextArea,
        max = 500
    )
    private String somethingElseDetails;

}
