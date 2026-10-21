package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationOutcomeTest {

    @ParameterizedTest
    @CsvSource({
        "grant, GRANTED",
        "suspend-warrant, GRANTED",
        "dismiss, DISMISSED",
        "refuse-warrant-suspension, DISMISSED",
        "strike-out, STRUCK_OUT",
        "list, LISTED",
        "other, OTHER"
    })
    void recordsTheOutcomeOfEachDecision(String decision, ApplicationOutcome outcome) {
        assertThat(ApplicationOutcome.of(decision)).isEqualTo(outcome);
    }

    @Test
    void rejectsAnOrderWithoutADecision() {
        assertThatThrownBy(() -> ApplicationOutcome.of(null))
            .hasMessageContaining("Select what the order decides about the application");
    }
}
