package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OccupationLicenceTypeWalesTest {

    @Test
    void shouldConvertFromCombinedLicenceType() {
        // When
        OccupationLicenceTypeWales occupationLicenceType
            = OccupationLicenceTypeWales.from(CombinedLicenceType.STANDARD_CONTRACT);

        // Then
        assertThat(occupationLicenceType).isEqualTo(OccupationLicenceTypeWales.STANDARD_CONTRACT);
    }

    @Test
    void shouldThrowErrorFromNonWalesLicenceType() {
        // When
        Throwable throwable = catchThrowable(() -> OccupationLicenceTypeWales.from(CombinedLicenceType.SECURE_TENANCY));

        // Then
        assertThat(throwable)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No value found for combined licence type: %s", CombinedLicenceType.SECURE_TENANCY);
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void shouldConvertNullCombinedLicenceTypeToNull() {
        // When
        OccupationLicenceTypeWales occupationLicenceType = OccupationLicenceTypeWales.from(null);

        // Then
        assertThat(occupationLicenceType).isNull();
    }

}
