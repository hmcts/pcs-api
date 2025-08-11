package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
//@ComplexType(name = "Defendant", generate = false)
public class Defendant {

    @CCD(label = "Do you know the defendant's name?")
    private VerticalYesNo defendantsNameKnown;

    @CCD(label = "Defendant's first name")
    private String firstName;

    @CCD(label = "Defendant's last name")
    private String lastName;

    @CCD(label = "Email")
    private String email;
}
