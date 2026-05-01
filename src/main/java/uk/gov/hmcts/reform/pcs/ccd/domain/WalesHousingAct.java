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
        label = "Are you an exempt landlord under Part 1 of the Housing (Wales) Act 2014?"
    )
    private VerticalYesNo isExemptLandlord;
}
