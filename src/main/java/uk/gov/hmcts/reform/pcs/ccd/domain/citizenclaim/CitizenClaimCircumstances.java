package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimCircumstances {

    @CCD(typeOverride = TextArea)
    private String defendantCircumstances;

    @CCD(typeOverride = TextArea)
    private String claimantCircumstances;

    private VerticalYesNo underlesseeExists;

    private String underlesseeName;

    private AddressUK underlesseeAddress;

}
