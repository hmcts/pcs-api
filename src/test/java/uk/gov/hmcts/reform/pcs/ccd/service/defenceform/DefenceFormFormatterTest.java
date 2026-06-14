package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormAddress;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatAdditionalContributionFrequency;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatIsoDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatLongDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.isYes;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.toFormAddress;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.toLabel;

class DefenceFormFormatterTest {

    @ParameterizedTest
    @MethodSource("gbpAmounts")
    void formatsGbpFromPoundsWithoutDivision(BigDecimal amount, String expected) {
        assertThat(formatGbp(amount)).isEqualTo(expected);
    }

    @Test
    void formatsLongDate() {
        assertThat(formatLongDate(LocalDate.of(2024, 1, 10))).isEqualTo("10 January 2024");
        assertThat(formatLongDate(null)).isNull();
    }

    @Test
    void formatsIsoDateString() {
        assertThat(formatIsoDate("2024-01-10")).isEqualTo("10 January 2024");
        assertThat(formatIsoDate(null)).isNull();
        assertThat(formatIsoDate("  ")).isNull();
    }

    @ParameterizedTest
    @MethodSource("additionalContributionFrequencies")
    void mapsAdditionalContributionFrequency(String raw, String expected) {
        assertThat(formatAdditionalContributionFrequency(raw)).isEqualTo(expected);
    }

    @Test
    void yesNoPredicatesAndLabelsAreNullSafe() {
        assertThat(isYes(VerticalYesNo.YES)).isTrue();
        assertThat(isNo(VerticalYesNo.NO)).isTrue();
        assertThat(isYes((VerticalYesNo) null)).isFalse();
        assertThat(isYes(YesNoNotSure.YES)).isTrue();
        assertThat(isNo(YesNoNotSure.NO)).isTrue();
        assertThat(isYes(YesNoNotSure.NOT_SURE)).isFalse();
        assertThat(toLabel(YesNoNotSure.NOT_SURE)).isEqualTo("I’m not sure");
        assertThat(toLabel((VerticalYesNo) null)).isNull();
        assertThat(DefenceFormFormatter.formatFrequency(RecurrenceFrequency.WEEKLY)).isEqualTo("Weekly");
        assertThat(DefenceFormFormatter.formatFrequency(null)).isNull();
    }

    @Test
    void mapsAddressEntityToFormAddress() {
        AddressEntity entity = AddressEntity.builder()
            .addressLine1("1 High St")
            .postTown("London")
            .postcode("AB1 2CD")
            .build();
        ClaimFormAddress address = toFormAddress(entity);
        assertThat(address.getAddressLine1()).isEqualTo("1 High St");
        assertThat(address.getPostTown()).isEqualTo("London");
        assertThat(address.getPostcode()).isEqualTo("AB1 2CD");
        assertThat(toFormAddress((AddressEntity) null)).isNull();
    }

    private static Stream<Arguments> gbpAmounts() {
        return Stream.of(
            Arguments.argumentSet("thousands", new BigDecimal("1500.00"), "£1,500.00"),
            Arguments.argumentSet("pence", new BigDecimal("12.34"), "£12.34"),
            Arguments.argumentSet("zero", BigDecimal.ZERO, "£0.00"),
            Arguments.argumentSet("null", null, null)
        );
    }

    private static Stream<Arguments> additionalContributionFrequencies() {
        return Stream.of(
            Arguments.argumentSet("weekly", "weekly", "Weekly"),
            Arguments.argumentSet("every2Weeks", "every2Weeks", "Every 2 weeks"),
            Arguments.argumentSet("every4Weeks", "every4Weeks", "Every 4 weeks"),
            Arguments.argumentSet("monthly", "monthly", "Monthly"),
            Arguments.argumentSet("unknown-rendered-blank", "fortnightly", null),
            Arguments.argumentSet("null", null, null)
        );
    }
}
