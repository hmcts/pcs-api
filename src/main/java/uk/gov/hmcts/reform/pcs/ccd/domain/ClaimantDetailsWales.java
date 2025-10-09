package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import java.time.LocalDate;

@Data
@Builder
public class ClaimantDetailsWales {

    @CCD(
        label = "Were you registered under Part 1 of the Housing (Wales) Act 2014?",
        access = {CitizenAccess.class}
    )
    private YesNoNotApplicable walesRegistrationLicensed;

    @CCD(
        label = "What's your registration number?",
        access = {CitizenAccess.class},
        max = 40
    )
    private String walesRegistrationNumber;

    @CCD(
        label = "Were you licensed under Part 1 of the Housing (Wales) Act 2014?",
        access = {CitizenAccess.class}
    )
    private YesNoNotApplicable walesLicenseLicensed;

    @CCD(
        label = "What's your licence number?",
        access = {CitizenAccess.class},
        max = 40
    )
    private String walesLicenseNumber;

    @CCD(
        label = "Have you appointed a licensed agent to be responsible for all the property management work "
            + "in relation to the dwelling as permitted under Part 1 of the Housing (Wales) Act 2014?",
        access = {CitizenAccess.class}
    )
    private YesNoNotApplicable walesLicensedAgentAppointed;

    @CCD(
        label = "Agent's first name",
        access = {CitizenAccess.class},
        max = 40
    )
    private String walesAgentFirstName;

    @CCD(
        label = "Agent's last name",
        access = {CitizenAccess.class},
        max = 40
    )
    private String walesAgentLastName;

    @CCD(
        label = "Agent's licence number",
        access = {CitizenAccess.class},
        max = 40
    )
    private String walesAgentLicenseNumber;

    @CCD(
        label = "Agent's date of appointment",
        hint = "For example, 16 4 2021",
        access = {CitizenAccess.class}
    )
    private LocalDate walesAgentAppointmentDate;
}
