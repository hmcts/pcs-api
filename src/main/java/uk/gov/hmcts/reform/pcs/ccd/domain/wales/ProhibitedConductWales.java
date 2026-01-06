package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProhibitedConductWales {

    @CCD(label = "Are you also making a claim for an order imposing a prohibited conduct standard contract?")
    private VerticalYesNo prohibitedConductWalesClaim;

    @CCD
    public PeriodicContractTermsWales periodicContractTermsWales;

    @CCD(
        label = "Why are you making this claim?",
        hint = "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String prohibitedConductWalesWhyMakingClaim;

    private VerticalYesNo claimForProhibitedConductContract;
    private VerticalYesNo agreedTermsOfPeriodicContract;
    private String detailsOfTerms;
    private String whyMakingClaim;

}

