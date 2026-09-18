package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedRadioList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemovePartyDetails {

    @JsonProperty("removeParty_PartyToRemove")
    @CCD(label = "",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList partyToRemove;

    @JsonProperty("removeParty_UnremovablePartyList")
    @CCD(label = "", typeOverride = FieldType.TextArea)
    private String unremovablePartyList;

    @JsonProperty("removeParty_LastPartyMessage")
    @CCD(label = "Which party are you removing?", typeOverride = FieldType.TextArea)
    private String lastPartyMessage;

    @JsonProperty("removeParty_CanSelectParty")
    @CCD
    private YesOrNo canSelectParty;

    @JsonProperty("removeParty_SelectedPartyLabel")
    @CCD(label = "Name")
    private String selectedPartyLabel;

    @JsonProperty("removeParty_PartyType")
    @CCD(label = "Party type",
        typeOverride = FixedRadioList,
        typeParameterOverride = "PartyType"
    )
    private PartyType partyType;

    @JsonProperty("removeParty_DateOfBirth")
    @CCD(label = "Date of birth")
    private String dateOfBirth;

    @JsonProperty("removeParty_Address")
    @CCD(label = "Address for service", typeOverride = FieldType.TextArea)
    private String address;

    @JsonProperty("removeParty_RemoveSelectedParty")
    @CCD(label = "Do you want to remove ${removeParty_SelectedPartyLabel}?")
    private YesOrNo removeSelectedParty;

}
