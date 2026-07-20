package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Arrays;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowConditions {

    public static final String NEVER_SHOW = "[STATE]=\"NEVER_SHOW\"";
    public static final String ENGLAND = "legislativeCountry=\"England\"";
    public static final String WALES = "legislativeCountry=\"Wales\"";

    public static String stateEquals(State state) {
        return "[STATE]=\"%s\"".formatted(state.name());
    }

    public static String stateNotEquals(State state) {
        return "[STATE]!=\"%s\"".formatted(state.name());
    }

    public static String fieldEquals(String fieldId, Enum<?> value) {
        return "%s=\"%s\"".formatted(fieldId, value.name());
    }

    public static String fieldContains(String fieldId, Enum<?> value) {
        return "%sCONTAINS\"%s\"".formatted(fieldId, value.name());
    }

    public static String and(String... conditions) {
        return String.join(" AND ", conditions);
    }

    public static String featureFlagsEnabled(FeatureFlag... featureFlags) {
        return Arrays.stream(featureFlags)
            .map(featureFlag -> {
                String name = getCcdFieldName(featureFlag);
                return "featureFlags.%s=\"YES\"".formatted(name);
            })
            .collect(Collectors.joining(" AND "));
    }

    private static String getCcdFieldName(FeatureFlag featureFlag) {
        return switch (featureFlag) {
            case RELEASE_1_DOT_2 -> "release1dot2Enabled";
            case CASEWORKER_EVENTS -> "caseWorkerEventsEnabled";
            default -> throw new IllegalArgumentException("Flag %s does not have a CCD field yet"
                                                              .formatted(featureFlag.name()));
        };
    }

}
