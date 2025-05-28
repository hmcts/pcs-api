package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PcsCase {

    @CCD
    private String userDetails;

    @CCD(label = "Case reference")
    private String hyphenatedCaseRef;

    @CCD(label = "Description of this case")
    private String caseDescription;

    @CCD
    private String pageHeadingMarkdown;

    @CCD(label = "Property details")
    private AddressUK claimAddress;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "PossessionGround"
    )
    private List<PossessionGround> groundsForPossession;

    @CCD(
        label = "Is there anything else you would like to add?",
        typeOverride = TextArea
    )
    private String generalNotes;

    @CCD(label = "Property postcode")
    private String claimPostcode;

    @CCD
    private YesOrNo showDraftsPage;

    @CCD
    private YesOrNo draftCaseSelected; // TODO: Delete this?

    @CCD(label = "Would you like to resume one of your incomplete claims?")
    private YesOrNo resumeDraftCase;

    @CCD(label = "Claim to resume")
    private DynamicList draftCases;

    // TODO: Clean way to identify fields from persistence vs derived fields?

    @CCD(label = "Breathing space in seconds")
    private String breathingSpaceSeconds;

}
