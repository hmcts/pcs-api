package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.time.LocalDate;
import java.util.Optional;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedRadioList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePartyDetails {

    @JsonProperty("updateParty_PartyToUpdate")
    @CCD(label = "Which party's contact information needs to be updated?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList partyToUpdate;

    @JsonProperty("updateParty_UpdatePartyType")
    @CCD(
        label = "Party type",
        typeOverride = FixedRadioList,
        typeParameterOverride = "PartyType"
    )
    private PartyType partyType;

    @JsonProperty("updateParty_UpdatePartyAddress")
    @CCD(label = "Address for service")
    private AddressUK address;

    @JsonProperty("updateParty_UpdatePartyEmail")
    @CCD(label = "Email address", typeOverride = FieldType.Email, searchable = false)
    private String email;

    @JsonProperty("updateParty_PhoneNumber")
    @CCD(label = "Phone number", regex = "^\\s*0\\d{10}\\s*$", searchable = false)
    private String phoneNumber;

    @JsonProperty("updateParty_DateOfBirth")
    @CCD(label = "Date of birth", hint = "For example, 16 4 2021", typeOverride = FieldType.Date)
    @JsonSerialize(using = EmptyStringLocalDateSerializer.class)
    private Optional<LocalDate> dateOfBirth;

    @JsonProperty("updateParty_PreviouslySelectedPartyId")
    @CCD(label = "Previously selected party")
    private String previouslySelectedPartyId;
}
