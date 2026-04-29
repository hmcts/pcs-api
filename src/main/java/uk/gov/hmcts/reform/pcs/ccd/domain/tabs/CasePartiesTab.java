package uk.gov.hmcts.reform.pcs.ccd.domain.tabs;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CasePartiesTab {

    @CCD (
        label = "Claimant"
    )
    private ClaimantTabDetails claimantDetails;

    @CCD(
        label = "Defendant"
    )
    private DefendantTabDetails defendantOneDetails;

    @CCD(
        label = "Additional defendant"
    )
    private List<ListValue<DefendantTabDetails>> defendantsDetails;
}
