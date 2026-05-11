package uk.gov.hmcts.reform.pcs.ccd.domain.tabs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemotionOfTenancyTabDetails {

    @CCD(
        label = "Section of the Housing Act demotion of tenancy claim made under"
    )
    private String housingAct;

    @CCD(
        label = "Have you served the defendants with a statement of the express terms which will "
            + "apply to the demoted tenancy?"
    )
    private String statementOfExpressTermsServed;

    @CCD(
        label = "Details of terms"
    )
    private String terms;

    @CCD(
        label = "Reasons for requesting a demotion of tenancy order"
    )
    private String reasons;
}
