package uk.gov.hmcts.reform.pcs.postcodecourt.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.SCOTLAND;

class StringToLegislativeCountryEnumConverterTest {

    private StringToLegislativeCountryEnumConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StringToLegislativeCountryEnumConverter();
    }

    @Test
    void shouldConvertFromStringToEnumAttribute() {
        LegislativeCountry converted = underTest.convert("Scotland");

        assertThat(converted).isEqualTo(SCOTLAND);
    }

}
