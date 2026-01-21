package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class VulnerableAdultsChildren {

    @CCD(
        label = "Confirm if the vulnerable people in the property are adults, children, or both adults and children"
    )
    private VulnerableCategory vulnerableCategory;

    @CCD(
        label = "How are they vulnerable?",
        hint = "You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String vulnerableReasonText;
}
