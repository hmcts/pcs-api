package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
