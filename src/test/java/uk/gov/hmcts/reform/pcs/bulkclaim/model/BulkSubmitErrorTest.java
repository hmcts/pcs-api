package uk.gov.hmcts.reform.pcs.bulkclaim.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulkSubmitErrorTest {

    @Test
    void shouldFormatAndParseRoundTrip() {
        BulkSubmitError error = new BulkSubmitError(6, "bulkClaimSubmissionId", "Unknown submission");

        assertThat(error.format()).isEqualTo("BULK|6|bulkClaimSubmissionId|Unknown submission");
        assertThat(BulkSubmitError.parse(error.format())).contains(error);
    }

    @Test
    void shouldNotLetSeparatorsInTextBreakParsing() {
        BulkSubmitError error = new BulkSubmitError(9, "a|b", "one|two");

        assertThat(BulkSubmitError.parse(error.format()))
            .contains(new BulkSubmitError(9, "a/b", "one/two"));
    }

    @Test
    void shouldIgnoreOtherCallbackErrors() {
        assertThat(BulkSubmitError.parse("Something else")).isEmpty();
        assertThat(BulkSubmitError.parse("BULK|x|path|message")).isEmpty();
        assertThat(BulkSubmitError.parse("BULK|6|path")).isEmpty();
        assertThat(BulkSubmitError.parse(null)).isEmpty();
    }
}
