package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

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
public class CitizenGenAppRequest implements GenAppRequest {

    private GenAppType applicationType;

    private VerticalYesNo within14Days;

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

    @CCD(max = 60)
    private String clientReference;

}
