package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

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

    /**
     * The assured grounds chosen, rent arrears grounds and other grounds together.
     */
    public Set<PrivateLandlordAssuredGround> allGrounds() {
        Set<PrivateLandlordAssuredGround> all = new LinkedHashSet<>();
        Stream.of(rentArrearsGrounds, otherGrounds)
            .filter(Objects::nonNull)
            .flatMap(Set::stream)
            .filter(Objects::nonNull)
            .forEach(all::add);
        return all;
    }

    /**
     * The reason given for each ground; the last one wins if a ground is listed twice.
     */
    public Map<PrivateLandlordAssuredGround, String> reasonsByGround() {
        Map<PrivateLandlordAssuredGround, String> reasons = new EnumMap<>(PrivateLandlordAssuredGround.class);
        if (groundReasons == null) {
            return reasons;
        }
        groundReasons.stream()
            .filter(Objects::nonNull)
            .map(ListValue::getValue)
            .filter(reason -> reason != null && reason.getGround() != null)
            .filter(reason -> StringUtils.isNotBlank(reason.getReason()))
            .forEach(reason -> reasons.put(reason.getGround(), reason.getReason()));
        return reasons;
    }

    /**
     * The names of the chosen Wales grounds, discretionary before mandatory.
     */
    public Set<String> walesGroundCodes() {
        Set<String> codes = new LinkedHashSet<>();
        if (walesDiscretionaryGrounds != null) {
            walesDiscretionaryGrounds.stream()
                .filter(Objects::nonNull).sorted().forEach(ground -> codes.add(ground.name()));
        }
        if (walesMandatoryGrounds != null) {
            walesMandatoryGrounds.stream()
                .filter(Objects::nonNull).sorted().forEach(ground -> codes.add(ground.name()));
        }
        return codes;
    }

    /**
     * The reason given for each Wales ground; the last one wins if a ground is listed twice.
     */
    public Map<String, String> walesReasonsByGround() {
        Map<String, String> reasons = new LinkedHashMap<>();
        if (walesGroundReasons == null) {
            return reasons;
        }
        walesGroundReasons.stream()
            .filter(Objects::nonNull)
            .map(ListValue::getValue)
            .filter(reason -> reason != null && StringUtils.isNotBlank(reason.getGround()))
            .filter(reason -> StringUtils.isNotBlank(reason.getReason()))
            .forEach(reason -> reasons.put(reason.getGround(), reason.getReason()));
        return reasons;
    }

    /**
     * Whether a Wales ground that is claimed for rent arrears is chosen: section 157 rent arrears, or the
     * mandatory section 181 or 187 grounds.
     */
    public boolean hasWalesRentArrearsGround() {
        return walesGroundCodes().stream().anyMatch(CitizenClaimGrounds::isWalesRentArrearsGround);
    }

    // Built per call rather than held in a static field, which the CCD SDK would generate as a case field
    public static boolean isWalesRentArrearsGround(String ground) {
        return Stream.of(DiscretionaryGroundWales.RENT_ARREARS_S157,
                         MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181,
                         MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187)
            .map(Enum::name)
            .anyMatch(name -> name.equals(ground));
    }

}
