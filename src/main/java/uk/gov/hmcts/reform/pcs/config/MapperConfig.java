package uk.gov.hmcts.reform.pcs.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

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

        modelMapper.createTypeMap(DocumentEntity.class, Document.class)
            .setConverter(createDocumentEntityConverter());

        return modelMapper;
    }

    private Converter<DocumentEntity, Document> createDocumentEntityConverter() {
        return context -> {
            DocumentEntity documentEntity = context.getSource();
            return Document.builder()
                    .filename(documentEntity.getFileName())
                    .url(documentEntity.getUrl())
                    .binaryUrl(documentEntity.getBinaryUrl())
                    .categoryId(documentEntity.getCategoryId())
                    .build();
        };
    }


}
