package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EnterGenAppRequest {

    @CCD(label = "What date was the application received?")
    private LocalDate dateReceived;

    @CCD(label = "Which type of application has the applicant made?")
    private EnterGenAppType applicationTypeOption;

    @CCD(
        label = "Which categories apply?",
        typeOverride = FieldType.TextArea
    )
    private String somethingElseDetails;

    @CCD(label = "Is there a hearing for this case in the next 14 days?")
    private VerticalYesNo within14Days;

    @CCD(label = "Has the applicant included a Help With Fees reference number on their application")
    private VerticalYesNo appliedForHwf;

    @CCD(
        label = "Enter their Help with Fees reference number",
        max = 60
    )
    private String hwfReference;

}
