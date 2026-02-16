package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SuspensionOfRightToBuy {

    public static final String SUSPENSION_OF_RIGHT_TO_BUY_REASON_LABEL = "Why are you requesting a suspension order?";

    @CCD(
        label = "Which section of the Housing Act is the suspension of right to buy claim made under?"
    )
    private SuspensionOfRightToBuyHousingAct housingAct;

    @CCD(
        label = SUSPENSION_OF_RIGHT_TO_BUY_REASON_LABEL,
        hint = "Give details of the defendants’ conduct and any other reasons you think are relevant. You can enter "
            + "up to 250 characters",
        typeOverride = TextArea
    )
    private String reason;

    private YesOrNo showHousingActsPage;
}
