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
public class AddEditDefendant {

    @CCD(label = "Do you know the defendant's name?")
    private VerticalYesNo defendantsNameKnown;

    @CCD(label = "Defendant's first name")
    private String firstName;
    @CCD(label = "Defendant's last name")
    private String lastName;
}
