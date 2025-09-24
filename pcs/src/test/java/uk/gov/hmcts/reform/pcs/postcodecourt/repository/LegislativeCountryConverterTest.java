package uk.gov.hmcts.reform.pcs.postcodecourt.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.NORTHERN_IRELAND;

class LegislativeCountryConverterTest {

    private LegislativeCountryConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegislativeCountryConverter();
    }

    @Test
    void shouldConvertFromEnumAttributeToStringLabel() {
        String converted = underTest.convertToDatabaseColumn(NORTHERN_IRELAND);

        assertThat(converted).isEqualTo("Northern Ireland");
    }

    @Test
    void shouldConvertFromDbStringToEnumAttribute() {
        LegislativeCountry converted = underTest.convertToEntityAttribute("Northern Ireland");

        assertThat(converted).isEqualTo(NORTHERN_IRELAND);
    }

}
