package uk.gov.hmcts.reform.pcs.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Converter<VerticalYesNo, Boolean> verticalYesNoToBoolean =
            context -> context.getSource() == null
                ? null : context.getSource().toBoolean();

        Converter<Boolean, VerticalYesNo> booleanToVerticalYesNo =
            context -> context.getSource() == null
                ? null : VerticalYesNo.from(context.getSource());

        modelMapper.createTypeMap(Boolean.class, VerticalYesNo.class)
            .setConverter(booleanToVerticalYesNo);

        modelMapper.createTypeMap(VerticalYesNo.class, Boolean.class)
            .setConverter(verticalYesNoToBoolean);

        return modelMapper;
    }

}
