package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ASBQuestionsDetailsWales {

    @CCD(
        searchable = false,
        label = "Is there actual or threatened antisocial behaviour?"
    )
    private VerticalYesNo antisocialBehaviour;

    @CCD(
        label = "Give details of the actual or threatened antisocial behaviour",
        hint = "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String antisocialBehaviourDetails;

    @CCD(
        searchable = false,
        label = "Is there actual or threatened use of the premises for illegal purposes?"
    )
    private VerticalYesNo illegalPurposesUse;

    @CCD(
        label = "Give details of the actual or threatened use of the premises for illegal purposes",
        hint = "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String illegalPurposesUseDetails;

    @CCD(
        searchable = false,
        label = "Has there been other prohibited conduct?"
    )
    private VerticalYesNo otherProhibitedConduct;

    @CCD(
        label = "Give details of the other prohibited conduct",
        hint = "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String otherProhibitedConductDetails;
}

