package uk.gov.hmcts.reform.pcs.ccd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClaimData {
    @CCD(ignore = true)
    private AddressUK propertyAddress;
    private LegislativeCountry legislativeCountry;
    private String feeAmount;
    private YesOrNo showCrossBorderPage;
    private YesOrNo showPropertyNotEligiblePage;
    private YesOrNo showPostcodeNotAssignedToCourt;
    @CCD(typeOverride = FieldType.DynamicRadioList)
    private DynamicStringList crossBorderCountriesList;
    private String crossBorderCountry1;
    private String crossBorderCountry2;
    private String postcodeNotAssignedView;
}
