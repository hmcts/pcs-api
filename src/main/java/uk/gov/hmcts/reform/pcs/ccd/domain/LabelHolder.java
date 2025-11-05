package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelHolder {

    @CCD(label = """
                ---
                <h2>Defendant's name</h2>
                Based on your previous answers, the amount per day that unpaid rent should be charged at is:
                <span class="govuk-body govuk-!-font-weight-bold">${formattedCalculatedDailyRentChargeAmount}</span>
                """, typeOverride = FieldType.Label)
    private String label;

    @CCD(label = "replace 2", typeOverride = FieldType.Label)
    private String label2;
}
