package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowConditions {

    public static final String NEVER_SHOW = "[STATE]=\"NEVER_SHOW\"";

    public static String isYes(String fieldName) {
        return "%s=\"Yes\"".formatted(fieldName);
    }

    // TODO: Javadoc
    public static String isNotYes(String fieldName) {
        return "%s!=\"Yes\"".formatted(fieldName);
    }

    // TODO: Tests
    public static String and(String... showConditions) {
        return String.join(" AND ", showConditions);
    }


}
