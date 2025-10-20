package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalChoice;

/**
 * The main domain model representing an enforcement order.
 */
@Builder
@Data
public class EnforcementOrder {

    @CCD(
        label = "What do you want to apply for?"
    )
    private SelectEnforcementType selectEnforcementType;

    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

    @CCD(
        label = """
        <ul>
          <li>Are violent or aggressive</li>
          <li>Possess a firearm or other weapon</li>
          <li>Have a history of criminal or antisocial behaviour</li>
          <li>Have made verbal or written threats towards you</li>
          <li>Are a member of a group that protests evictions</li>
          <li>Have had visits from the police or social services</li>
          <li>Own an aggressive dog or other animal</li>
        </ul>
        """,
        typeOverride = FieldType.Label
    )
    private String riskFactorsList;

    @CCD(
        label = "Does anyone living at the property pose a risk to the bailiff?"
    )
    private VerticalChoice confirmLivingAtProperty;

}
