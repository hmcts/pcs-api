package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.Text;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WalesNoticeDetails {

    @CCD(label = "Have you served notice to the defendants?")
    private YesOrNo noticeServed;

    @CCD(
        label = "What type of notice did you serve?",
        hint = "For example, Form RHW20",
        typeOverride = Text,
        max = 10
    )
    private String typeOfNoticeServed;
}
