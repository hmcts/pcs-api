package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
@Builder
public class LegalCosts {

    @CCD(
            label = "Do you want to reclaim any legal costs?",
            hint =  "For example, explain if there is a communal entrance to "
                    + "the property and include the entry code. If the property has a car park or a designated "
                    + "parking space, tell the bailiff where they can park their car"
    )
    private VerticalYesNo areLegalCostsToBeClaimed;

    @CCD(
            label = "How much do you want to reclaim?",
            typeOverride = FieldType.MoneyGBP
    )
    private String amountOfLegalCosts;
}
