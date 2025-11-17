package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

/**
 * Use this class to annotate fields that exist in {@link PCSCase} that
 * should not be persisted as draft data, (e.g. derived fields)
 */
@SuppressWarnings("unused")
public abstract class DraftCaseDataMixIn {

    @JsonIgnore
    private YesOrNo decentralised;
    @JsonIgnore
    private YesOrNo showCrossBorderPage;
    @JsonIgnore
    private String caseTitleMarkdown;
    @JsonIgnore
    private String nextStepsMarkdown;

    @JsonIgnore
    private String claimantName;
    @JsonIgnore
    private String claimantContactEmail;

}
