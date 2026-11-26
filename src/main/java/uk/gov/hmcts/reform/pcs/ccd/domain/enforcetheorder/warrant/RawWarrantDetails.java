package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicMultiSelectList;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RawWarrantDetails {

    public static final String SELECTED_DEFENDANTS_LABEL = "Who do you want to evict?";

    public static final String VULNERABLE_PEOPLE_LABEL = "Is anyone living at the property vulnerable?";

    @CCD(
            label = VULNERABLE_PEOPLE_LABEL
    )
    private YesNoNotSure vulnerablePeoplePresent;

    private VulnerableAdultsChildren vulnerableAdultsChildren;

    @CCD(
            label = SELECTED_DEFENDANTS_LABEL,
            typeOverride = DynamicMultiSelectList
    )
    private DynamicMultiSelectStringList selectedDefendants;
}
