package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonCompatibilityTest {

    @Test
    void applicationMapperUsesJackson2() {
        assertThat(new JacksonConfiguration().getMapper().getClass().getName())
            .startsWith("com.fasterxml.jackson.");
    }

    @Test
    void applicationMapperRoundTripsCaseData() throws Exception {
        ObjectMapper mapper = new JacksonConfiguration().getMapper();
        PCSCase source = PCSCase.builder().feeAmount("100").build();

        PCSCase restored = mapper.readValue(mapper.writeValueAsString(source), PCSCase.class);

        assertThat(restored.getFeeAmount()).isEqualTo("100");
    }

    @Test
    void draftCaseDataMapperUsesJackson2() {
        ObjectMapper mapper = new JacksonConfiguration().draftCaseDataObjectMapper();

        assertThat(mapper.getClass().getName()).startsWith("com.fasterxml.jackson.");
    }

    @Test
    @SuppressWarnings("removal")
    void preferredHttpConvertersUseJackson2() {
        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                Jackson2AutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class))
            .withUserConfiguration(JacksonConfiguration.class)
            .withPropertyValues("spring.http.converters.preferred-json-mapper=jackson2")
            .run(context -> {
                ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
                ServerHttpMessageConvertersCustomizer customizer = context
                    .getBean("jackson2HttpMessageConvertersCustomizer", ServerHttpMessageConvertersCustomizer.class);
                HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().disableDefaults();
                customizer.customize(builder);
                var jackson2Converters = StreamSupport.stream(builder.build().spliterator(), false)
                    .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                    .toList();

                assertThat(jackson2Converters).isNotEmpty();
                assertThat(jackson2Converters)
                    .extracting(converter -> ((MappingJackson2HttpMessageConverter) converter).getObjectMapper())
                    .contains(objectMapper);
            });
    }
}
