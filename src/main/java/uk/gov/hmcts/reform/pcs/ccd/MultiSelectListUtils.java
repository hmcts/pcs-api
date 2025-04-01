package uk.gov.hmcts.reform.pcs.ccd;

import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;

import java.util.List;
import java.util.UUID;

public class MultiSelectListUtils {

    public static List<UUID> getSelectedCodes(DynamicMultiSelectList multiSelectList) {

        if (multiSelectList == null) {
            return List.of();
        }

        return multiSelectList.getValue().stream()
            .map(DynamicListElement::getCode)
            .toList();
    }

}
