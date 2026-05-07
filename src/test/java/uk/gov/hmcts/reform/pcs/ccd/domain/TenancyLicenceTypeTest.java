package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TenancyLicenceTypeTest {

    @Test
    void shouldConvertFromCombinedLicenceType() {
        // When
        TenancyLicenceType tenancyLicenceType = TenancyLicenceType.from(CombinedLicenceType.SECURE_TENANCY);

        // Then
        assertThat(tenancyLicenceType).isEqualTo(TenancyLicenceType.SECURE_TENANCY);
    }

    @Test
    void shouldThrowErrorFromWalesLicenceType() {
        // When
        Throwable throwable = catchThrowable(() -> TenancyLicenceType.from(CombinedLicenceType.STANDARD_CONTRACT));

        // Then
        assertThat(throwable)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No value found for combined licence type: %s", CombinedLicenceType.STANDARD_CONTRACT);
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void shouldConvertNullCombinedLicenceTypeToNull() {
        // When
        TenancyLicenceType tenancyLicenceType = TenancyLicenceType.from(null);

        // Then
        assertThat(tenancyLicenceType).isNull();
    }

}
