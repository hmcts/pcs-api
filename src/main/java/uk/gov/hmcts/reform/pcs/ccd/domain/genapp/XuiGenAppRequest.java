package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class XuiGenAppRequest implements GenAppRequest {

    @CCD(label = "What do you want to apply for?")
    private GenAppType applicationType;

    @CCD(
        label = "Is the defendant’s court hearing in the next 14 days?",
        hint = "This will affect the fee you will invoice to the defendant. "
             + "They will not need to pay a fee if their court hearing is (at least) 14 days away.")
    private VerticalYesNo within14Days;

    @CCD(label = "Does the defendant need help paying the fee for this application?")
    private VerticalYesNo needHwf;

    @CCD(label = "Have they already applied for help with their application fee?")
    private VerticalYesNo appliedForHwf;

    @CCD(
        label = "Enter their Help with Fees reference number",
        hint = "The defendant will have received this number when they applied for Help with Fees. This reference "
             + "must not have been used for a previous application. For example, HWF-A1B-23C",
        max = 60
    )
    private String hwfReference;

    @CCD(
        label = "Have the other parties agreed to this application?",
        hint = "If you ask the court for more than one thing, this answer will apply to all of them"
    )
    private VerticalYesNo otherPartiesAgreed;

    @CCD(label = "Are there any reasons that this application should not be shared with the other parties?")
    private VerticalYesNo withoutNotice;

    @CCD(
        label = "Provide the reason this application should not be shared with the other party",
        typeOverride = FieldType.TextArea,
        max = 6800
    )
    private String withoutNoticeReason;

    @CCD(
        label = "Explain what the defendant wants the court to do, and why",
        typeOverride = FieldType.TextArea,
        max = 6800
    )
    private String whatOrderWanted;

    private VerticalYesNo hasSupportingDocuments;

    private List<ListValue<UploadedDocument>> uploadedDocuments;

    @CCD(label = "Which language did you use to complete this service?")
    private LanguageUsed languageUsed;

    private VerticalYesNo sotAccepted;

    @CCD(max = 100)
    private String sotFullName;

    @CCD(searchable = false)
    private String standardFee;

    @CCD(searchable = false)
    private String maxFee;

    @CCD(searchable = false)
    private VerticalYesNo showHwfScreens;

}
