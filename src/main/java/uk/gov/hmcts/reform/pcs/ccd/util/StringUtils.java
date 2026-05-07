package uk.gov.hmcts.reform.pcs.ccd.util;

import java.util.List;

public class StringUtils {

    public static String joinIfNotEmpty(String delimiter, List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        return String.join(delimiter, elements);
    }

}
