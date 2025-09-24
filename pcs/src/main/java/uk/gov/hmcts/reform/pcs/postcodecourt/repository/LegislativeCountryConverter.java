package uk.gov.hmcts.reform.pcs.postcodecourt.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Converter(autoApply = true)
public class LegislativeCountryConverter implements AttributeConverter<LegislativeCountry, String> {

    @Override
    public String convertToDatabaseColumn(LegislativeCountry attribute) {
        return attribute.getLabel();
    }

    @Override
    public LegislativeCountry convertToEntityAttribute(String dbData) {
        return LegislativeCountry.fromLabel(dbData);
    }

}
