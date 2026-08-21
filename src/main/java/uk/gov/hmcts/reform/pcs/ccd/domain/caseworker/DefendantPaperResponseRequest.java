package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ContactPreferencesSelection;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;

import java.time.LocalDate;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantPaperResponseRequest {

    @JsonProperty("paperResponse_FreeLegalAdvice")
    @CCD(label = "Has the defendant had any free legal advice")
    private YesNoPreferNotToSay freeLegalAdvice;

    @JsonProperty("paperResponse_FirstName")
    @CCD(label = "First name(s)")
    private String firstName;

    @JsonProperty("paperResponse_LastName")
    @CCD(label = "Last name")
    private String lastName;

    @JsonProperty("paperResponse_DateOfBirth")
    @CCD(label = "1.2 What is the defendant’s date of birth?")
    private LocalDate dateOfBirth;

    @JsonProperty("paperResponse_Address")
    @CCD(label = "1.3 What is the defendant’s address for service?")
    private AddressUK address;

    @JsonProperty("paperResponse_ContactPreferences")
    @CCD(
        label = "How does the defendant want to receive updates about their case?",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "ContactPreferencesSelection"
    )
    private Set<ContactPreferencesSelection> contactPreferences;

    @JsonProperty("paperResponse_EmailAddress")
    @CCD(
        label = "Defendant’s email address",
        typeOverride = FieldType.Email
    )
    private String emailAddress;

    @JsonProperty("paperResponse_PhoneNumber")
    @CCD(
        label = "The defendant’s phone number is",
        regex = "^\\s*0\\d{10}\\s*$",
        max = 60
    )
    private String phoneNumber;

    @JsonProperty("paperResponse_HasMadeCounterClaim")
    @CCD(
        label = "15.1 Has the defendant made a counterclaim against the claimant?"
    )
    private VerticalYesNo hasMadeCounterClaim;
}
