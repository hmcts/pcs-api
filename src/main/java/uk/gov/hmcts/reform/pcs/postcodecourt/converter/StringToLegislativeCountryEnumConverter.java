package uk.gov.hmcts.reform.pcs.postcodecourt.converter;

import org.springframework.core.convert.converter.Converter;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

public class StringToLegislativeCountryEnumConverter implements Converter<String, LegislativeCountry> {

    @Override
    public LegislativeCountry convert(String source) {
        return LegislativeCountry.fromLabel(source);
    }

}
