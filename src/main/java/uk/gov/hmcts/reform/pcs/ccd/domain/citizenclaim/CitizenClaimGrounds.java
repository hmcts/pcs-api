package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;

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

}
