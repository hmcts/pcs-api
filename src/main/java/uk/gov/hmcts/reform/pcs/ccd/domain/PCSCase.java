package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

/**
 * The CCD domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {
    @CCD(label = "Description of this case")
    private String caseDescription;

    // Markdown string making up the 'example' tab in XUI.
    // Set dynamically upon case load
    private String exampleTabMarkdown;

    @CCD(label = "Party first name")
    private String partyFirstName;
    @CCD(label = "Party last name")
    private String partyLastName;

    private AddressUK propertyAddress;

    @CCD(label = "Case state")
    private String state;

    @CCD(label = "Country")
    private String country;

    @CCD(label = "Is this name correct?")
    private YesOrNo isNameCorrect;

    private String organisationName;

    @CCD(label = "Enter the updated organisation name")
    private String editOrganisationName;

    @CCD(label = "Use the same email address for notifications related to this claim?")
    private YesOrNo useSameEmail;

    @CCD(label = "Type yo")
    private ClaimType claimType;

    @CCD(
        label = "Choose claim type",
        typeOverride = FieldType.DynamicRadioList
    )
    private DynamicList claimTypes;

    @CCD(label = "Claimant organisation type")
    private ClaimantType claimantType;

    @CCD(
        label = "Circumstances of the claim",
        typeOverride = FieldType.DynamicRadioList
    )
    private DynamicList evictionType;

}
