package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.List;

/**
 * The CCD domain model representing a possessions case.
 */
@Builder
@Data
public class PcsCase {

    @CCD(ignore = true)
    @JsonIgnore
    private Long ccdCaseReference;

    @CCD(label = "Case reference")
    private String hyphenatedCaseRef;

    @CCD(label = "Description of this case")
    private String caseDescription;

    @CCD(label = "Property address",
        access = DefaultAccess.class)
    private AddressUK propertyAddress;

    @CCD(label = "Applicant's first name",
        access = DefaultAccess.class)
    private String applicantForename;

    @CCD(label = "Active Parties",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party",
        access = DefaultAccess.class
    )
    private List<ListValue<Party>> activeParties;

    @CCD
    private YesOrNo activePartiesEmpty;

    @CCD(label = "Inactive Parties",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party",
        access = DefaultAccess.class
    )
    private List<ListValue<Party>> inactiveParties;

    @CCD(access = DefaultAccess.class)
    private YesOrNo inactivePartiesEmpty;

    @CCD(label = "Party",
        access = DefaultAccess.class)
    private Party currentParty;

    @CCD(label = "Parties",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party",
        access = DefaultAccess.class
    )
    private List<ListValue<Party>> partiesToAdd;

    @CCD(
        label = "Select parties to deactivate",
        access = DefaultAccess.class
    )
    private DynamicMultiSelectList partiesToDeactivate;

    @CCD(
        label = "Select parties to reactivate",
        access = DefaultAccess.class
    )
    private DynamicMultiSelectList partiesToReactivate;

    @CCD(ignore = true,
        access = DefaultAccess.class)
    @JsonIgnore
    private List<Claim> claims;

    @CCD(label = "Claim details",
        access = DefaultAccess.class)
    private Claim claimToAdd;

    @CCD(
        label = "Claimants",
        access = DefaultAccess.class
    )
    private DynamicMultiSelectList claimantsToAdd;

    @CCD(
        label = "Defendants",
        access = DefaultAccess.class
    )
    private DynamicMultiSelectList defendantsToAdd;

    @CCD(
        label = "Interested Parties",
        access = DefaultAccess.class
    )
    private DynamicMultiSelectList interestedPartiesToAdd;

    @CCD(typeOverride = FieldType.YesOrNo,
        access = DefaultAccess.class)
    private YesOrNo ipEmpty;

    @CCD(typeOverride = FieldType.YesOrNo,
        access = DefaultAccess.class)
    private YesOrNo partyListEmpty;

    @CCD(label = "Current claim ID",
        access = DefaultAccess.class)
    private String currentClaimId;

    @CCD(access = DefaultAccess.class)
    private String claimsSummaryMarkdown;

    @CCD(access = DefaultAccess.class)
    private String partyRolesMarkdown;

    public void addClaim(Claim claim) {
        claims.add(claim);
    }

}
