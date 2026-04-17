package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenGenAppRequest {

    private GenAppType applicationType;

    private YesOrNo within14Days;

    private YesOrNo needHwf;

    private YesOrNo appliedForHwf;

    @CCD(max = 16)
    private String hwfReference;

    private YesOrNo otherPartiesAgreed;

    private YesOrNo withoutNotice;

    @CCD(max = 6800)
    private String withoutNoticeReason;

    @CCD(max = 6800)
    private String whatOrderWanted;

    private LanguageUsed languageUsed;

}
