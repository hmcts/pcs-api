package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasonsForPossessionTabDetails {

    @CCD(label = "Reasons for claiming possession under ground 1")
    private String ground1;

    @CCD(label = "Reasons for claiming possession under ground 2")
    private String ground2;

    @CCD(label = "Reasons for claiming possession under ground 2A")
    private String ground2A;

    @CCD(label = "Reasons for claiming possession under ground 2ZA")
    private String ground2ZA;

    @CCD(label = "Reasons for claiming possession under ground 3")
    private String ground3;

    @CCD(label = "Reasons for claiming possession under ground 4")
    private String ground4;

    @CCD(label = "Reasons for claiming possession under ground 5")
    private String ground5;

    @CCD(label = "Reasons for claiming possession under ground 6")
    private String ground6;

    @CCD(label = "Reasons for claiming possession under ground 7")
    private String ground7;

    @CCD(label = "Reasons for claiming possession under ground 7A")
    private String ground7A;

    @CCD(label = "Reasons for claiming possession under ground 7B")
    private String ground7B;

    @CCD(label = "Reasons for claiming possession under ground 8")
    private String ground8;

    @CCD(label = "Reasons for claiming possession under ground 9")
    private String ground9;

    @CCD(label = "Reasons for claiming possession under ground 10")
    private String ground10;

    @CCD(label = "Reasons for claiming possession under ground 10A")
    private String ground10A;

    @CCD(label = "Reasons for claiming possession under ground 11")
    private String ground11;

    @CCD(label = "Reasons for claiming possession under ground 12")
    private String ground12;

    @CCD(label = "Reasons for claiming possession under ground 13")
    private String ground13;

    @CCD(label = "Reasons for claiming possession under ground 14")
    private String ground14;

    @CCD(label = "Reasons for claiming possession under ground 14A")
    private String ground14A;

    @CCD(label = "Reasons for claiming possession under ground 14ZA")
    private String ground14ZA;

    @CCD(label = "Reasons for claiming possession under ground 15")
    private String ground15;

    @CCD(label = "Reasons for claiming possession under ground 15A")
    private String ground15A;

    @CCD(label = "Reasons for claiming possession under ground 16")
    private String ground16;

    @CCD(label = "Reasons for claiming possession under ground 17")
    private String ground17;

    @CCD(label = "Reasons for claiming possession under ground A")
    private String groundA;

    @CCD(label = "Reasons for claiming possession under ground B")
    private String groundB;

    @CCD(label = "Reasons for claiming possession under ground C")
    private String groundC;

    @CCD(label = "Reasons for claiming possession under ground D")
    private String groundD;

    @CCD(label = "Reasons for claiming possession under ground E")
    private String groundE;

    @CCD(label = "Reasons for claiming possession under ground F")
    private String groundF;

    @CCD(label = "Reasons for claiming possession under ground G")
    private String groundG;

    @CCD(label = "Reasons for claiming possession under ground H")
    private String groundH;

    @CCD(label = "Reasons for claiming possession under ground I")
    private String groundI;

    @CCD(label = "Reasons for claiming possession under Condition 1 of Section 84A of the Housing Act 1985")
    private String condition1OfSection84A;

    @CCD(label = "Reasons for claiming possession under Condition 2 of Section 84A of the Housing Act 1985")
    private String condition2OfSection84A;

    @CCD(label = "Reasons for claiming possession under Condition 3 of Section 84A of the Housing Act 1985")
    private String condition3OfSection84A;

    @CCD(label = "Reasons for claiming possession under Condition 4 of Section 84A of the Housing Act 1985")
    private String condition4OfSection84A;

    @CCD(label = "Reasons for claiming possession under Condition 5 of Section 84A of the Housing Act 1985")
    private String condition5OfSection84A;

    @CCD(label = "Reasons for claiming possession under section 157")
    private String section157;

    @CCD(label = "Reasons for claiming possession under section 170")
    private String section170;

    @CCD(label = "Reasons for claiming possession under section 178")
    private String section178;

    @CCD(label = "Reasons for claiming possession under section 181")
    private String section181;

    @CCD(label = "Reasons for claiming possession under section 186")
    private String section186;

    @CCD(label = "Reasons for claiming possession under section 187")
    private String section187;

    @CCD(label = "Reasons for claiming possession under section 191")
    private String section191;

    @CCD(label = "Reasons for claiming possession under section 199")
    private String section199;

    @CCD(label = "Reasons for claiming possession under paragraph 25B(2) of Schedule 12")
    private String paragraph25B2Schedule12;

    @CCD(label = "Reasons for claiming possession under Antisocial behaviour")
    private String antisocialBehaviour;

    @CCD(label = "Reasons for claiming possession under Breach of the tenancy")
    private String breachOfTheTenancy;

    @CCD(label = "Reasons for claiming possession under Absolute grounds")
    private String absoluteGrounds;

    @CCD(label = "Reasons for claiming possession under Other grounds")
    private String otherGrounds;

    @CCD(label = "Reasons for claiming possession under No grounds")
    private String noGrounds;

    @CCD(label = "Additional reasons for possession")
    private String additionalReasonsForPossession;

    @CCD(label = "Do you have any additional reasons for possession?")
    private String hasAdditionalReasons;
    
    @CCD(label = "Details of additional reasons")
    private String additionalReasonsDetails;
}
