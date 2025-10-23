package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import java.util.stream.Stream;

class RiskCategoryTestUtil {

    static Stream<String> validTextScenarios() {
        return Stream.of(
                "A",
                "A".repeat(6800)
        );
    }

    static Stream<String> invalidTextScenarios() {
        return Stream.of(
                null,
                "   ",
                ""
        );
    }
}
