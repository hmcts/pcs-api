package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class LegalCosts {

    @CCD(
            label = "Do you want to reclaim any legal costs?"
    )
    private VerticalYesNo areLegalCostsToBeClaimed;

    @CCD(
            label = "How much do you want to reclaim?",
            typeOverride = FieldType.MoneyGBP,
            min = 1
    )
    private String amountOfLegalCosts;
}
