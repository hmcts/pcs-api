package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Defendants {

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant1;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant2;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant3;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant4;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant5;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant6;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant7;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant8;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant9;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant10;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant11;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant12;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant13;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant14;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant15;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant16;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant17;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant18;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant19;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant20;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant21;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant22;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant23;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant24;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant25;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant1;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant2;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant3;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant4;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant5;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant6;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant7;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant8;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant9;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant10;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant11;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant12;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant13;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant14;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant15;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant16;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant17;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant18;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant19;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant20;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant21;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant22;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant23;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant24;

    @CCD(
        label = "Do you need to add another defendant?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo addAnotherDefendant25;
}
