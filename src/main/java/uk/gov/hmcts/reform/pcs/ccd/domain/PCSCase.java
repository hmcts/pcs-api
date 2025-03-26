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
public class PCSCase {

    @CCD(ignore = true)
    @JsonIgnore
    private Long ccdCaseReference;

    @CCD(label = "Case reference")
    private String hyphenatedCaseRef;

    @CCD(label = "Description of this case")
    private String caseDescription;

    @CCD(label = "Property address")
    private AddressUK propertyAddress;

    @CCD(label = "Applicant's first name")
    private String applicantForename;

    // TODO: Add a note to the CCD SDK README about using ListValue for Collection type?
    @CCD(label = "Claimants",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party"
    )
    private List<ListValue<Party>> claimants;

    @CCD(label = "Defendants",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party"
    )
    private List<ListValue<Party>> defendants;

    @CCD(label = "Interested Parties",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party"
    )
    private List<ListValue<Party>> interestedParties;

    @CCD(label = "Active Parties",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party"
    )
    private List<ListValue<Party>> activeParties;

    @CCD(typeOverride = FieldType.YesOrNo)
    private YesOrNo activePartiesEmpty;

    @CCD(label = "Inactive Parties",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party"
    )
    private List<ListValue<Party>> inactiveParties;

    @CCD(typeOverride = FieldType.YesOrNo)
    private YesOrNo inactivePartiesEmpty;

    @CCD(label = "Parties",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Party"
    )
    private List<ListValue<Party>> partiesToAdd;

    @CCD(
        label = "Select parties to deactivate"
    )
    private DynamicMultiSelectList partiesToDeactivate;

    @CCD(
        label = "Select parties to reactivate"
    )
    private DynamicMultiSelectList partiesToReactivate;

}
