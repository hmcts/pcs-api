package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import java.util.stream.Stream;

class RiskCategoryTestUtil {

    static Stream<String> validTextScenarios() {
        return Stream.of(
                "A",
                "Short text",
                "The defendant is a risk towards others",
                "A".repeat(1000),
                "A".repeat(5000),
                "A".repeat(6799),
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
