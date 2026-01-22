package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NonPrefixWarrantDetails {

    @CCD(
            label = "Is anyone living at the property vulnerable?"
    )
    private YesNoNotSure vulnerablePeoplePresent;

    private VulnerableAdultsChildren vulnerableAdultsChildren;
}
