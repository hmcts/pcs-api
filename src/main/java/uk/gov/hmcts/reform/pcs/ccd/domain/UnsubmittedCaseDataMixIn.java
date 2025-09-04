package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

/**
 * Use this class to annotate fields that exist in {@link PCSCase} that
 * should not be persisted as draft data, (e.g. derived fields)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class UnsubmittedCaseDataMixIn {

    @JsonIgnore
    private YesOrNo decentralised;
    @JsonIgnore
    private YesOrNo showCrossBorderPage;
    @JsonIgnore
    private String pageHeadingMarkdown;
    @JsonIgnore
    private String claimPaymentTabMarkdown;
    @JsonIgnore
    private String nextStepsMarkdown;

    @JsonIgnore
    private String claimantName;
    @JsonIgnore
    private String claimantContactEmail;

}
