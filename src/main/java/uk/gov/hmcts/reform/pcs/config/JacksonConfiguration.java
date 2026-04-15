package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DraftCaseDataMixIn;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesOrNoMixin;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.MapperFeature.INFER_BUILDER_TYPE_BINDINGS;

@Configuration
public class JacksonConfiguration {

    @Primary
    @Bean
    public ObjectMapper getMapper() {
        ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .enable(INFER_BUILDER_TYPE_BINDINGS)
            .disable(AUTO_CLOSE_JSON_CONTENT)
            .defaultPropertyInclusion(JsonInclude.Value.ALL_NON_NULL)
            .addMixIn(YesOrNo.class, YesOrNoMixin.class)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .addModules(new Jdk8Module(), new JavaTimeModule(), new ParameterNamesModule())
            .build();

        mapper.setDateFormat(new StdDateFormat());

        return mapper;
    }

    @Bean
    public ObjectMapper draftCaseDataObjectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
            .disable(AUTO_CLOSE_JSON_CONTENT)
            .defaultPropertyInclusion(JsonInclude.Value.ALL_NON_NULL)
            .addMixIn(YesOrNo.class, YesOrNoMixin.class)
            .addMixIn(PCSCase.class, DraftCaseDataMixIn.class)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .addModules(new Jdk8Module(), new JavaTimeModule(), new ParameterNamesModule())
            .build();

        mapper.setDateFormat(new StdDateFormat());

        mapper.configOverride(ArrayNode.class).setMergeable(false);

        return mapper;
    }
}
