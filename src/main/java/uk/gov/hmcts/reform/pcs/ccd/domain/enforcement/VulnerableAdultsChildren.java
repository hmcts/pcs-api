package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

@Data
@Builder
@ComplexType(generate = true)
public class VulnerableAdultsChildren {

    public static final String VULNERABLE_PEOPLE_YES_NO_LABEL = "Is anyone living at the property vulnerable?";
    public static final String VULNERABLE_CATEGORY_LABEL =
            "Confirm if the vulnerable people in the property are adults, children, or both adults and children";
    public static final String VULNERABLE_REASON_LABEL = "How are they vulnerable?";
    public static final String VULNERABLE_REASON_HINT = "You can enter up to 6,800 characters.";
    public static final int VULNERABLE_REASON_TEXT_LIMIT = 6800;

    @CCD(
            label = VULNERABLE_PEOPLE_YES_NO_LABEL
    )
    private YesNoNotSure vulnerablePeopleYesNo;

    @CCD(
            label = VULNERABLE_CATEGORY_LABEL
    )
    private VulnerableCategory vulnerableCategory;

    @CCD(
            label = VULNERABLE_REASON_LABEL,
            hint = VULNERABLE_REASON_HINT,
            typeOverride = FieldType.TextArea
    )
    private String vulnerableReasonText;
}
