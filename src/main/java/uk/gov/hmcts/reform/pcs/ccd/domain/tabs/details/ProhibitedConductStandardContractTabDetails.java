package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProhibitedConductStandardContractTabDetails {

    @CCD(label = "Are you seeking an order imposing a prohibited conduct standard contract?")
    private String seekingProhibitedConductStandardContract;

    @CCD(
        label = "Have you and the contract holder agreed terms of the periodic standard contract"
            + " in addition to those incorporated by statute?"
    )
    private String agreedTerms;

    @CCD(
        label = "Details of terms",
        typeOverride = TextArea
    )
    private String termDetails;

    @CCD(
        label = "Why are you making this claim?",
        typeOverride = TextArea
    )
    private String whyMakingClaim;

}
