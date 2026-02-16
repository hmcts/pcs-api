package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDate;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WalesHousingAct {

    @CCD(
        label = "Were you registered under Part 1 of the Housing (Wales) Act 2014?"
    )
    private YesNoNotApplicable registered;

    @CCD(
        label = "What’s your registration number?",
        max = 60
    )
    private String registrationNumber;

    @CCD(
        label = "Were you licensed under Part 1 of the Housing (Wales) Act 2014?"
    )
    private YesNoNotApplicable licensed;

    @CCD(
        label = "What’s your licence number?",
        max = 60
    )
    private String licenceNumber;

    @CCD(
        label = "Have you appointed a licensed agent to be responsible for all the property management work "
            + "in relation to the dwelling as permitted under Part 1 of the Housing (Wales) Act 2014?"
    )
    private YesNoNotApplicable licensedAgentAppointed;

    @CCD(
        label = "Agent’s first name",
        max = 60
    )
    private String agentFirstName;

    @CCD(
        label = "Agent’s last name",
        max = 60
    )
    private String agentLastName;

    @CCD(
        label = "Agent’s licence number",
        max = 60
    )
    private String agentLicenceNumber;

    @CCD(
        label = "Agent’s date of appointment",
        hint = "For example, 16 4 2021"
    )
    private LocalDate agentAppointmentDate;
}
