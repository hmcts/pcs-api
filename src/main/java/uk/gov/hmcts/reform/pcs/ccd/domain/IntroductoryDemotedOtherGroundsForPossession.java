package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;
import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.OTHER_GROUND_DESCRIPTION_LABEL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class IntroductoryDemotedOtherGroundsForPossession {

    @CCD(
        label = "Do you have grounds for possession?"
    )
    private VerticalYesNo hasIntroductoryDemotedOtherGroundsForPossession;

    @CCD(
        label = "What are your grounds for possession?",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "IntroductoryDemotedOrOtherGrounds"
    )
    private Set<IntroductoryDemotedOrOtherGrounds> introductoryDemotedOrOtherGrounds;

    @CCD(
        label = OTHER_GROUND_DESCRIPTION_LABEL,
        hint = "You’ll be able to explain your reasons for claiming possession"
            + " under these grounds on the next screen. You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String otherGroundDescription;
}
