package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedRadioList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddPartyDetails {

    @JsonProperty("addParty_ManagePartyOptions")
    @CCD(label = "What change do you want to make?",
        hint = "You must have judicial approval to add or remove a party",
        typeOverride = FixedRadioList,
        typeParameterOverride = "ManagePartyOptions"
    )
    private ManagePartyOptions managePartyOptions;

    @JsonProperty("addParty_AddPartyType")
    @CCD(
        label = "Which type of party are you adding?",
        typeOverride = FixedRadioList,
        typeParameterOverride = "PartyType"
    )
    private PartyType addPartyType;

    @JsonProperty("addParty_ClaimantOrganisationName")
    @CCD(label = "Organisation name")
    private String claimantOrganisationName;

    @JsonProperty("addParty_ClaimantFirstName")
    @CCD(label = "First name", max = 60)
    private String claimantFirstName;

    @JsonProperty("addParty_ClaimantLastName")
    @CCD(label = "Last name", max = 60)
    private String claimantLastName;

    @JsonProperty("addParty_ClaimantAddress")
    @CCD(label = "Address for service")
    private AddressUK claimantAddress;

    @JsonProperty("addParty_ClaimantEmail")
    @CCD(label = "Email address", typeOverride = FieldType.Email, searchable = false)
    private String claimantEmail;

    @JsonProperty("addParty_ClaimantPhoneNumber")
    @CCD(label = "Phone number", regex = "^\\s*0\\d{10}\\s*$", searchable = false)
    private String claimantPhoneNumber;

    @JsonProperty("addParty_FirstName")
    @CCD(label = "First name", max = 60)
    private String firstName;

    @JsonProperty("addParty_LastName")
    @CCD(label = "Last name", max = 60)
    private String lastName;

    @JsonProperty("addParty_DefendantDateOfBirth")
    @CCD(label = "Date of birth", hint = "For example, 16 4 2021")
    private LocalDate defendantDateOfBirth;

    @JsonProperty("addParty_DefendantAddress")
    @CCD(label = "Address for service")
    private AddressUK defendantAddress;

    @JsonProperty("addParty_DefendantEmail")
    @CCD(label = "Email address", typeOverride = FieldType.Email, searchable = false)
    private String defendantEmail;

    @JsonProperty("addParty_DefendantPhoneNumber")
    @CCD(label = "Phone number", regex = "^\\s*0\\d{10}\\s*$", searchable = false)
    private String defendantPhoneNumber;

    @JsonProperty("addParty_LFFirstName")
    @CCD(label = "First name", max = 60)
    private String litigationFriendFirstName;

    @JsonProperty("addParty_LFLastName")
    @CCD(label = "Last name", max = 60)
    private String litigationFriendLastName;

    @JsonProperty("addParty_LFOrganisationName")
    @CCD(label = "Organisation name")
    private String litigationFriendOrganisationName;

    @JsonProperty("addParty_LFAddress")
    @CCD(label = "Address for service")
    private AddressUK litigationFriendAddress;

    @JsonProperty("addParty_LFEmail")
    @CCD(label = "Email address", typeOverride = FieldType.Email, searchable = false)
    private String litigationFriendEmail;

    @JsonProperty("addParty_LFPhoneNumber")
    @CCD(label = "Phone number", regex = "^\\s*0\\d{10}\\s*$", searchable = false)
    private String litigationFriendPhoneNumber;

    @JsonProperty("addParty_PartyRadioList")
    @CCD(label = "Which party is the litigation friend acting for?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList partyRadioList;
}
