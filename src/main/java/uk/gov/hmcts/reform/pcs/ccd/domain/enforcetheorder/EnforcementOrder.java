package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;

/**
 * The main domain model representing an enforcement order.
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnforcementOrder {

    @CCD(
        label = "What do you want to apply for?"
    )
    private SelectEnforcementType selectEnforcementType;

    @JsonUnwrapped
    @CCD
    private WarrantDetails warrantDetails;

    @CCD(
        searchable = false
    )
    @External
    private String warrantFeeAmount;

    @CCD(
        searchable = false
    )
    @External
    private String writFeeAmount;
}
