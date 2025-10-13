package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDate;

@Data
@Builder
public class WalesHousingAct {

    @CCD(
        label = "Were you registered under Part 1 of the Housing (Wales) Act 2014?"
    )
    private YesNoNotApplicable walesRegistered;

    @CCD(
        label = "What's your registration number?",
        max = 40
    )
    private String walesRegistrationNumber;

    @CCD(
        label = "Were you licensed under Part 1 of the Housing (Wales) Act 2014?"
    )
    private YesNoNotApplicable walesLicensed;

    @CCD(
        label = "What's your licence number?",
        max = 40
    )
    private String walesLicenceNumber;

    @CCD(
        label = "Have you appointed a licensed agent to be responsible for all the property management work "
            + "in relation to the dwelling as permitted under Part 1 of the Housing (Wales) Act 2014?"
    )
    private YesNoNotApplicable walesLicensedAgentAppointed;

    @CCD(
        label = "Agent's first name",
        max = 40
    )
    private String walesAgentFirstName;

    @CCD(
        label = "Agent's last name",
        max = 40
    )
    private String walesAgentLastName;

    @CCD(
        label = "Agent's licence number",
        max = 40
    )
    private String walesAgentLicenceNumber;

    @CCD(
        label = "Agent's date of appointment",
        hint = "For example, 16 4 2021"
    )
    private LocalDate walesAgentAppointmentDate;
}
