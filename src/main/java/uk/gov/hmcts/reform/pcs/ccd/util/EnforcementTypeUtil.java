package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnforcementTypeUtil {

    public static DynamicStringListElement convertToDynamicStringListElement(SelectEnforcementType type) {
        return DynamicStringListElement.builder()
                .code(type.name())
                .label(type.getLabel())
                .build();
    }

    public static DynamicStringList createDynamicStringList(List<SelectEnforcementType> types) {
        final DynamicStringList enforcementTypes = new DynamicStringList();

        List<DynamicStringListElement> listItems = new ArrayList<>();
        for (SelectEnforcementType type : types) {
            listItems.add(convertToDynamicStringListElement(type));
        }
        enforcementTypes.setListItems(listItems);

        return enforcementTypes;
    }
}