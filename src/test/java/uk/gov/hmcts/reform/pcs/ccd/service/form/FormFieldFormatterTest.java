package uk.gov.hmcts.reform.pcs.ccd.service.form;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormAddress;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatLongDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatOrdinalDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatShortTime;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatUkDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isYes;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.toFormAddress;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.toLabel;

class FormFieldFormatterTest {

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
    void formatsUkDateFromUtcTimestampAndIsNullSafe() {
        Clock ukClock = Clock.fixed(Instant.parse("2026-07-16T08:00:00Z"), ZoneId.of("Europe/London"));
        // 23:30 UTC on 15 July = 00:30 BST on 16 July - the UK calendar day is the 16th.
        assertThat(formatUkDate(LocalDateTime.of(2026, 7, 15, 23, 30), ukClock)).isEqualTo(LocalDate.of(2026, 7, 16));
        // Winter (GMT): no offset, same calendar day.
        assertThat(formatUkDate(LocalDateTime.of(2026, 1, 5, 12, 0), ukClock)).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(formatUkDate(null, ukClock)).isNull();
    }

    @Test
    void isPopulatedIsNullAndBlankSafe() {
        assertThat(isPopulated("x")).isTrue();
        assertThat(isPopulated(null)).isFalse();
        assertThat(isPopulated("  ")).isFalse();
    }

    @Test
    void yesNoPredicatesAndLabelsAreNullSafe() {
        assertThat(isYes(VerticalYesNo.YES)).isTrue();
        assertThat(isNo(VerticalYesNo.NO)).isTrue();
        assertThat(isYes((VerticalYesNo) null)).isFalse();
        assertThat(toLabel((VerticalYesNo) null)).isNull();
        assertThat(isYes(YesNoNotSure.YES)).isTrue();
        assertThat(isNo(YesNoNotSure.NO)).isTrue();
        assertThat(isYes(YesNoNotSure.NOT_SURE)).isFalse();
        assertThat(toLabel(YesNoNotSure.NOT_SURE)).isEqualTo("I’m not sure");
        assertThat(toLabel((YesNoNotSure) null)).isNull();
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

    @Test
    void mapsAddressUkToFormAddress() {
        AddressUK uk = AddressUK.builder()
            .addressLine1("9 New Road")
            .postTown("Leeds")
            .postCode("LS1 1AA")
            .build();
        ClaimFormAddress address = toFormAddress(uk);
        assertThat(address.getAddressLine1()).isEqualTo("9 New Road");
        assertThat(address.getPostTown()).isEqualTo("Leeds");
        assertThat(address.getPostcode()).isEqualTo("LS1 1AA");
        assertThat(toFormAddress((AddressUK) null)).isNull();
    }

    private static Stream<Arguments> gbpAmounts() {
        return Stream.of(
            Arguments.argumentSet("thousands", new BigDecimal("1500.00"), "£1,500.00"),
            Arguments.argumentSet("pence", new BigDecimal("12.34"), "£12.34"),
            Arguments.argumentSet("zero", BigDecimal.ZERO, "£0.00"),
            Arguments.argumentSet("null", null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("shortTimes")
    void shouldFormatShortTime(LocalDateTime dateTime, String expected) {
        assertThat(formatShortTime(dateTime)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("ordinalDates")
    void shouldFormatOrdinalDate(LocalDate date, String expected) {
        assertThat(formatOrdinalDate(date)).isEqualTo(expected);
    }

    private static Stream<Arguments> shortTimes() {
        return Stream.of(
            Arguments.argumentSet("with minutes", LocalDateTime.of(2026, 8, 15, 15, 40), "3:40pm"),
            Arguments.argumentSet("on the hour", LocalDateTime.of(2026, 8, 15, 10, 0), "10am"),
            Arguments.argumentSet("midday", LocalDateTime.of(2026, 8, 15, 12, 0), "12pm"),
            Arguments.argumentSet("after midnight", LocalDateTime.of(2026, 8, 15, 0, 5), "12:05am"),
            Arguments.argumentSet("null", null, null)
        );
    }

    private static Stream<Arguments> ordinalDates() {
        return Stream.of(
            Arguments.argumentSet("1st", LocalDate.of(2026, 8, 1), "1st August 2026"),
            Arguments.argumentSet("2nd", LocalDate.of(2026, 8, 2), "2nd August 2026"),
            Arguments.argumentSet("3rd", LocalDate.of(2026, 8, 3), "3rd August 2026"),
            Arguments.argumentSet("11th", LocalDate.of(2026, 8, 11), "11th August 2026"),
            Arguments.argumentSet("12th", LocalDate.of(2026, 8, 12), "12th August 2026"),
            Arguments.argumentSet("13th", LocalDate.of(2026, 8, 13), "13th August 2026"),
            Arguments.argumentSet("15th", LocalDate.of(2026, 8, 15), "15th August 2026"),
            Arguments.argumentSet("21st", LocalDate.of(2026, 8, 21), "21st August 2026"),
            Arguments.argumentSet("22nd", LocalDate.of(2026, 8, 22), "22nd August 2026"),
            Arguments.argumentSet("23rd", LocalDate.of(2026, 8, 23), "23rd August 2026"),
            Arguments.argumentSet("31st", LocalDate.of(2026, 8, 31), "31st August 2026"),
            Arguments.argumentSet("null", null, null)
        );
    }
}
