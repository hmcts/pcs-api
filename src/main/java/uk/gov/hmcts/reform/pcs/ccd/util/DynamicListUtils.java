package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;
import java.util.function.Function;

@UtilityClass
public class DynamicListUtils {

    public static DynamicListElement retainSelectedValue(DynamicListElement selected,
                                                         List<DynamicListElement> options) {
        return retainSelectedValue(selected, options, DynamicListElement::getCode, DynamicListElement::getLabel);
    }

    public static DynamicStringListElement retainSelectedValue(DynamicStringListElement selected,
                                                               List<DynamicStringListElement> options) {
        return retainSelectedValue(
            selected,
            options,
            DynamicStringListElement::getCode,
            DynamicStringListElement::getLabel
        );
    }

    private static <T> T retainSelectedValue(T selected,
                                             List<T> options,
                                             Function<T, Object> code,
                                             Function<T, String> label) {
        Object selectedCode = selected == null ? null : code.apply(selected);
        String selectedLabel = selected == null ? null : label.apply(selected);

        if (selectedCode == null) {
            return selectedLabel == null ? null : matchingOptionByLabel(selectedLabel, options, label);
        }
        return options.stream()
            .filter(option -> selectedCodeMatches(selectedCode, code.apply(option))
                || selectedLabel != null && selectedLabel.equals(label.apply(option)))
            .findFirst()
            .orElse(null);
    }

    private static <T> T matchingOptionByLabel(String selectedLabel, List<T> options, Function<T, String> label) {
        return options.stream()
            .filter(option -> selectedLabel.equals(label.apply(option)))
            .findFirst()
            .orElse(null);
    }

    private static boolean selectedCodeMatches(Object selectedCode, Object optionCode) {
        return optionCode != null && selectedCode.toString().equals(optionCode.toString());
    }
}
