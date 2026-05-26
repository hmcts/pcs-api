package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
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

    private VerticalYesNo appliedForHwf;

    @CCD(max = 100)
    private String hwfReference;

    private VerticalYesNo otherPartiesAgreed;

    private VerticalYesNo withoutNotice;

    @CCD(max = 6800)
    private String withoutNoticeReason;

    @CCD(max = 6800)
    private String whatOrderWanted;

    private VerticalYesNo hasSupportingDocuments;

    private List<ListValue<UploadedDocument>> uploadedDocuments;

    private LanguageUsed languageUsed;

    private VerticalYesNo sotAccepted;

    @CCD(max = 100)
    private String sotFullName;

    @CCD(searchable = false)
    private String standardFee;

    @CCD(searchable = false)
    private String maxFee;

}
