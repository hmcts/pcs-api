package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantCircumstanceTabDetails {

    @CCD(
        label = "Is there any information you’re required to provide, or you want to provide, "
            + "about the defendants’ circumstances?"
    )
    private String defendantCircumstancesGiven;

    @CCD(label = "Defendants’ circumstances")
    private String defendantCircumstances;
}
