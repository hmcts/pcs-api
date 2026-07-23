package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedRadioList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddParty {

    @CCD(label = "What change do you want to make",
        hint = "You must have judicial approval to add or remove a party",
        typeOverride = FixedRadioList,
        typeParameterOverride = "ManagePartyOptions"
    )
    private ManagePartyOptions managePartyOptions;

    @CCD(
        label = "Which type of party are you adding?",
        typeOverride = FixedRadioList,
        typeParameterOverride = "PartyType"
    )
    private PartyType addPartyType;

    @CCD(label = "Organisation name")
    private String organisationName;

    @CCD(label = "Claimant name")
    private String claimantName;

    @CCD(label = "First name")
    private String firstName;

    @CCD(label = "Last name")
    private String lastName;

    @CCD(label = "Litigation friend name")
    private String litigationFriendName;

    @CCD(label = "Date of birth")
    private LocalDate dateOfBirth;

    @CCD(label = "Address for service")
    @External
    private AddressUK address;

    @CCD(label = "Email address", searchable = false)
    private String email;

    @CCD(label = "Phone number", searchable = false)
    private String phoneNumber;
}
