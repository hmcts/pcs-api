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
public class CalculatedDailyRentLabel {

    @CCD(label = """
        ---
        Label 1:
        <section tabindex="0">
            <p class="govuk-body">
                Based on your previous answers, the amount per day that unpaid
                rent should be charged at is:
                <span class="govuk-body govuk-!-font-weight-bold">
                    ${formattedCalculatedDailyRentChargeAmount}
                </span>
            </p>
        </section>
        """, typeOverride = FieldType.Label)
    private String label1;

    @CCD(label = "", typeOverride = FieldType.Label)
    private String label2;
}
