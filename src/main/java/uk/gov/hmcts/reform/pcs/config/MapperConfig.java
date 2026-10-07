package uk.gov.hmcts.reform.pcs.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.UUID;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration().setAmbiguityIgnored(true);

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

        Converter<UUID, String> uuidToString =
            context -> context.getSource() == null ? null : context.getSource().toString();

        modelMapper.createTypeMap(PartyEntity.class, Party.class)
            .addMappings(mapper -> mapper.using(uuidToString)
                .map(PartyEntity::getId, Party::setId));

        return modelMapper;
    }

}
