package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimGrounds {

    private VerticalYesNo hasRentArrearsGrounds;

    @CCD(typeOverride = MultiSelectList, typeParameterOverride = "PrivateLandlordAssuredGround")
    private Set<PrivateLandlordAssuredGround> rentArrearsGrounds;

    private VerticalYesNo hasOtherGrounds;

    @CCD(typeOverride = MultiSelectList, typeParameterOverride = "PrivateLandlordAssuredGround")
    private Set<PrivateLandlordAssuredGround> otherGrounds;

    @CCD(typeOverride = TextArea)
    private String otherGroundsDetails;

    private List<ListValue<CitizenClaimGroundReason>> groundReasons;

    // Excluded tenancy only: replaces the assured grounds questions above
    private VerticalYesNo hasExcludedGrounds;

    @CCD(typeOverride = MultiSelectList, typeParameterOverride = "IntroductoryDemotedOrOtherGrounds")
    private Set<IntroductoryDemotedOrOtherGrounds> excludedGrounds;

    @CCD(typeOverride = TextArea)
    private String excludedOtherGroundDetails;

    @CCD(typeOverride = TextArea)
    private String excludedReason;
    // Asked instead of the England grounds for a property in Wales; "Others" is kept in otherGrounds
    @CCD(typeOverride = MultiSelectList, typeParameterOverride = "DiscretionaryGroundWales")
    private Set<DiscretionaryGroundWales> walesDiscretionaryGrounds;

    @CCD(typeOverride = MultiSelectList, typeParameterOverride = "MandatoryGroundWales")
    private Set<MandatoryGroundWales> walesMandatoryGrounds;

    private List<ListValue<CitizenClaimWalesGroundReason>> walesGroundReasons;

}
