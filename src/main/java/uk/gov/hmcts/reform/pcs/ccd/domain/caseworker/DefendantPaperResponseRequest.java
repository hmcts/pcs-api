package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DefendantPaperResponseRequest {

    @CCD(label = "Has the defendant had any free legal advice")
    private YesNoPreferNotToSay freeLegalAdvice;

    @CCD(label = "First name(s)")
    private String firstName;

    @CCD(label = "Last name")
    private String lastName;

    @CCD(label = "1.2 What is the defendant’s date of birth?")
    private LocalDate dateOfBirth;

    @CCD(label = "1.3 what is the defendant’s address for service?")
    private AddressUK address;

    @CCD(
        label = "How does the defendant want to receive updates about their case?",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "ContactPreferencesSelection"
    )
    private Set<ContactPreferencesSelection> contactPreferences;

    @CCD(
        label = "Defendant’s email address",
        typeOverride = FieldType.Email
    )
    private String emailAddress;

    @CCD(
        label = "2.2 If we need to contact the defendant with notifications or urgent updates about their case, "
            + "what is their phone number?",
        regex = "^\\s*0\\d{10}\\s*$",
        max = 60
    )
    private String phoneNumber;

    @CCD(
        label = "15.1 Has the defendant made a counterclaim against the claimant?"
    )
    private VerticalYesNo hasMadeCounterClaim;
}
