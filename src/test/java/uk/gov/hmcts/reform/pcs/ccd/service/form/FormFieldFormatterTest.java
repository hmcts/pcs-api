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
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatLongDate;
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
}
