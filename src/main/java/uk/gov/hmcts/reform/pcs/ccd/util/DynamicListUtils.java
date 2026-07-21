package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;

@UtilityClass
public class DynamicListUtils {

    public static DynamicListElement retainSelectedValue(DynamicListElement selected,
                                                         List<DynamicListElement> options) {
        if (selected == null || selected.getCode() == null) {
            if (selected == null || selected.getLabel() == null) {
                return null;
            }
            return options.stream()
                .filter(option -> selected.getLabel().equals(option.getLabel()))
                .findFirst()
                .orElse(null);
        }

        return options.stream()
            .filter(option -> selected.getCode().toString().equals(option.getCode().toString())
                || selected.getLabel() != null && selected.getLabel().equals(option.getLabel()))
            .findFirst()
            .orElse(null);
    }

    public static DynamicStringListElement retainSelectedValue(DynamicStringListElement selected,
                                                               List<DynamicStringListElement> options) {
        if (selected == null || selected.getCode() == null) {
            if (selected == null || selected.getLabel() == null) {
                return null;
            }
            return options.stream()
                .filter(option -> selected.getLabel().equals(option.getLabel()))
                .findFirst()
                .orElse(null);
        }

        return options.stream()
            .filter(option -> selected.getCode().equals(option.getCode())
                || selected.getLabel() != null && selected.getLabel().equals(option.getLabel()))
            .findFirst()
            .orElse(null);
    }
}
