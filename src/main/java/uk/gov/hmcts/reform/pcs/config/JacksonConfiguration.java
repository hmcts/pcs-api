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
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.ccd.domain.DraftCaseDataMixIn;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesOrNoMixin;

import java.lang.reflect.Type;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.MapperFeature.INFER_BUILDER_TYPE_BINDINGS;

@Configuration
public class JacksonConfiguration {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder
            .configure(tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .enable(tools.jackson.databind.MapperFeature.INFER_BUILDER_TYPE_BINDINGS)
            .disable(tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(tools.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion(
                inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .addMixIn(FeeLookupResponseDto.class, FeeLookupResponseDtoMixIn.class);
    }

    @tools.jackson.databind.annotation.JsonNaming(
        tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class
    )
    private abstract static class FeeLookupResponseDtoMixIn {
    }

    @Configuration(proxyBeanMethods = false)
    public static class LegacyFeignConfiguration {

        @Bean
        @SuppressWarnings("removal")
        public HttpMessageConverterCustomizer legacyFeignJacksonConverter(ObjectMapper objectMapper) {
            return converters -> converters.add(0, new LegacyFeignJacksonHttpMessageConverter(objectMapper));
        }
    }

    @SuppressWarnings("removal")
    private static class LegacyFeignJacksonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

        private LegacyFeignJacksonHttpMessageConverter(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        public boolean canRead(Class<?> type, org.springframework.http.MediaType mediaType) {
            return isLegacyResponse(type) && super.canRead(type, mediaType);
        }

        @Override
        public boolean canRead(Type type, Class<?> contextClass, org.springframework.http.MediaType mediaType) {
            return type instanceof Class<?> rawType
                && isLegacyResponse(rawType)
                && super.canRead(type, contextClass, mediaType);
        }

        @Override
        public boolean canWrite(Class<?> type, org.springframework.http.MediaType mediaType) {
            return isLegacyRequest(type) && super.canWrite(type, mediaType);
        }

        @Override
        public boolean canWrite(Type type, Class<?> contextClass, org.springframework.http.MediaType mediaType) {
            boolean isLegacyType = type instanceof Class<?> rawType && isLegacyRequest(rawType);
            boolean isLegacyContext = contextClass != null && isLegacyRequest(contextClass);
            return (isLegacyType || isLegacyContext)
                && super.canWrite(type, contextClass, mediaType);
        }

        private static boolean isLegacyResponse(Class<?> type) {
            Class<?> valueType = type.isArray() ? type.getComponentType() : type;
            String packageName = valueType.getPackageName();
            return packageName.startsWith("uk.gov.hmcts.reform.fees.client")
                || packageName.startsWith("uk.gov.hmcts.reform.payments")
                || packageName.startsWith("uk.gov.hmcts.reform.ccd.document.am");
        }

        private static boolean isLegacyRequest(Class<?> type) {
            return isLegacyResponse(type)
                || type.getName().equals(
                    "uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest"
                );
        }
    }

    @Primary
    @Bean
    public ObjectMapper getMapper() {
        ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .enable(INFER_BUILDER_TYPE_BINDINGS)
            .disable(AUTO_CLOSE_JSON_CONTENT)
            .defaultPropertyInclusion(JsonInclude.Value.ALL_NON_NULL)
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
