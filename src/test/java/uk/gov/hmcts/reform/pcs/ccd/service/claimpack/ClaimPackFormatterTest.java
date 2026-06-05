package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatDefendantHeading;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatUnderlesseeHeading;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isYes;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.toLabel;

class ClaimPackFormatterTest {

    @ParameterizedTest
    @MethodSource("gbpAmounts")
    void formatsGbpCurrency(BigDecimal amount, String expected) {
        assertThat(formatGbp(amount)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("populatedStrings")
    void detectsPopulatedText(String text, boolean expected) {
        assertThat(isPopulated(text)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("defendantHeadings")
    void numbersDefendantHeadings(int number, String expected) {
        assertThat(formatDefendantHeading(number)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("underlesseeHeadings")
    void numbersUnderlesseeHeadings(int number, String expected) {
        assertThat(formatUnderlesseeHeading(number)).isEqualTo(expected);
    }

    @Test
    void yesNoPredicatesAreNullSafe() {
        assertThat(isYes(VerticalYesNo.YES)).isTrue();
        assertThat(isYes(VerticalYesNo.NO)).isFalse();
        assertThat(isYes(null)).isFalse();
        assertThat(isNo(VerticalYesNo.NO)).isTrue();
        assertThat(isNo(null)).isFalse();
    }

    @Test
    void toLabelIsNullSafeAndTitleCase() {
        assertThat(toLabel(VerticalYesNo.YES)).isEqualTo("Yes");
        assertThat(toLabel((VerticalYesNo) null)).isNull();
    }

    private static Stream<Arguments> gbpAmounts() {
        return Stream.of(
            Arguments.argumentSet("thousands", new BigDecimal("1500.00"), "£1,500.00"),
            Arguments.argumentSet("pence", new BigDecimal("12.34"), "£12.34"),
            Arguments.argumentSet("zero", BigDecimal.ZERO, "£0.00"),
            Arguments.argumentSet("null", null, null)
        );
    }

    private static Stream<Arguments> populatedStrings() {
        return Stream.of(
            Arguments.argumentSet("null", null, false),
            Arguments.argumentSet("empty", "", false),
            Arguments.argumentSet("blank", "   ", false),
            Arguments.argumentSet("text", "x", true)
        );
    }

    private static Stream<Arguments> defendantHeadings() {
        return Stream.of(
            Arguments.argumentSet("first", 1, "Defendant 1 details"),
            Arguments.argumentSet("second", 2, "Additional defendant 1 details"),
            Arguments.argumentSet("third", 3, "Additional defendant 2 details")
        );
    }

    private static Stream<Arguments> underlesseeHeadings() {
        return Stream.of(
            Arguments.argumentSet("first", 1, "Underlessee or mortgagee 1 details"),
            Arguments.argumentSet("second", 2, "Additional underlessee or mortgagee 1 details")
        );
    }
}
