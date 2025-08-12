package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;

import java.util.List;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private final YesOrNo decentralised = YesOrNo.YES;

    @CCD(
        label = "Claimant Name",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String claimantName;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo isClaimantNameCorrect;

    @CCD(
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String overriddenClaimantName;

    @CCD(
        label = "Property address",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private AddressUK propertyAddress;

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private YesOrNo showCrossBorderPage;
    @CCD(
        typeOverride = DynamicRadioList,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private DynamicStringList crossBorderCountriesList;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String crossBorderCountry1;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String crossBorderCountry2;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class}
    )
    @External
    private String userPcqId;

    @CCD(searchable = false, access = {CitizenAccess.class})
    private YesOrNo userPcqIdSet;

    @CCD(
        label = "Case management location",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private Integer caseManagementLocation;

    @CCD(
        label = "Payment status",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private PaymentStatus paymentStatus;

    @CCD(
        label = "Amount to pay",
        hint = "£400",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private PaymentType paymentType;

    @CCD(ignore = true)
    @JsonIgnore
    private List<ListValue<Claim>> claims;

    @CCD(label = "Party")
    private List<ListValue<Party>> parties;

    private String pageHeadingMarkdown;

    private String claimPaymentTabMarkdown;

    @CCD(
        label = "Legislative country",
        access = CaseworkerAccess.class
    )
    private LegislativeCountry legislativeCountryChoice;

    private String legislativeCountry;

    @CCD(
        label = "Who is the claimant in this case?",
        hint = "If you’re a legal representative, you should select the type of claimant you’re representing.",
        typeOverride = DynamicRadioList,
        access = {CaseworkerAccess.class}
    )
    private DynamicStringList claimantType;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimantTypeNotEligibleEngland;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimantTypeNotEligibleWales;

    @CCD(
        label = "Is this a claim against trespassers?",
        access = CaseworkerAccess.class
    )
    private VerticalYesNo claimAgainstTrespassers;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimTypeNotEligibleEngland;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimTypeNotEligibleWales;

}
