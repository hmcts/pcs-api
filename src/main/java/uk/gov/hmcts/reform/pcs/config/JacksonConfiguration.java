package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.DraftCaseDataMixIn;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.MapperFeature.INFER_BUILDER_TYPE_BINDINGS;

@Configuration
public class JacksonConfiguration {

    /**
     * Mix-in to override Party's @JsonInclude(ALWAYS) annotation for draft persistence.
     * Party needs ALWAYS for CCD token validation, but drafts need NON_NULL for PATCH semantics.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private interface DraftPartyMixIn {}

    /**
     * Mix-in to override AddressUK's serialization for draft persistence.
     * Ensures null address fields are omitted to prevent overwriting existing data.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private interface DraftAddressMixIn {}

    @Primary
    @Bean
    public ObjectMapper getMapper() {
        ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .enable(INFER_BUILDER_TYPE_BINDINGS)
            .disable(AUTO_CLOSE_JSON_CONTENT)
            .serializationInclusion(Include.NON_NULL)
            .build();

        JavaTimeModule datetime = new JavaTimeModule();
        mapper.registerModule(datetime);

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat());

        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.registerModules(new Jdk8Module(), new JavaTimeModule(), new ParameterNamesModule());

        return mapper;
    }

    @Bean
    public ObjectMapper draftCaseDataObjectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
            .disable(AUTO_CLOSE_JSON_CONTENT)
            .build();

        mapper.addMixIn(PCSCase.class, DraftCaseDataMixIn.class);
        mapper.setSerializationInclusion(Include.NON_NULL);

        // HDPI-3509: Override Party's @JsonInclude(ALWAYS) for draft persistence
        // Party needs ALWAYS for CCD token validation in START callback,
        // but drafts need NON_NULL to prevent null fields from overwriting existing data
        mapper.addMixIn(Party.class, DraftPartyMixIn.class);
        mapper.addMixIn(AddressUK.class, DraftAddressMixIn.class);

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat());

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.registerModules(new Jdk8Module(), new JavaTimeModule(), new ParameterNamesModule());

        mapper.configOverride(ArrayNode.class).setMergeable(false);

        return mapper;
    }
}
