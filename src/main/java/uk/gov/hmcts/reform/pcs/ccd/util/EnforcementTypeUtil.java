package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnforcementTypeUtil {

    public static DynamicStringListElement convertToDynamicStringListElement(SelectEnforcementType type) {
        return DynamicStringListElement.builder()
                .code(type.name())
                .label(type.getLabel())
                .build();
    }
}