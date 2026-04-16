package uk.gov.hmcts.reform.pcs.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.ccd.domain.SimpleYesNo;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Converter<SimpleYesNo, Boolean> verticalYesNoToBoolean =
            context -> context.getSource() == null
                ? null : context.getSource().toBoolean();

        Converter<Boolean, SimpleYesNo> booleanToVerticalYesNo =
            context -> context.getSource() == null
                ? null : SimpleYesNo.from(context.getSource());

        modelMapper.createTypeMap(Boolean.class, SimpleYesNo.class)
            .setConverter(booleanToVerticalYesNo);

        modelMapper.createTypeMap(SimpleYesNo.class, Boolean.class)
            .setConverter(verticalYesNoToBoolean);

        return modelMapper;
    }

}
