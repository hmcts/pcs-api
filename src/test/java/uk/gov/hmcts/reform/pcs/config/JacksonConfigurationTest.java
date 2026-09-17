package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("removal")
class JacksonConfigurationTest {

    @Test
    void shouldDeserializeLegacyFeesClientSnakeCasePropertiesWithJacksonThree() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonConfiguration().jsonMapperBuilderCustomizer().customize(builder);

        FeeLookupResponseDto response = builder.build().readValue(
            """
                {
                  "code": "FEE0412",
                  "description": "Issue a possession claim",
                  "fee_amount": 404.00,
                  "version": 1
                }
                """,
            FeeLookupResponseDto.class
        );

        assertThat(response.getFeeAmount()).isEqualByComparingTo("404.00");
    }

    @Test
    void shouldUseJacksonTwoForLegacyFeignClientModels() throws Exception {
        JacksonConfiguration configuration = new JacksonConfiguration();
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        new JacksonConfiguration.LegacyFeignConfiguration()
            .legacyFeignJacksonConverter(configuration.getMapper())
            .accept(converters);
        var converter = (MappingJackson2HttpMessageConverter) converters.getFirst();
        var input = new MockHttpInputMessage(
            """
                {
                  "code": "FEE0412",
                  "description": "Issue a possession claim",
                  "fee_amount": 404.00,
                  "version": 1
                }
                """.getBytes(StandardCharsets.UTF_8)
        );
        input.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        FeeLookupResponseDto response = (FeeLookupResponseDto) converter.read(FeeLookupResponseDto.class, input);

        assertThat(response.getFeeAmount()).isEqualByComparingTo("404.00");
        assertThat(converter.canWrite(CreateServiceRequestDTO.class, MediaType.APPLICATION_JSON)).isTrue();
        assertThat(converter.canWrite(
            CreateServiceRequestDTO.class,
            CreateServiceRequestDTO.class,
            MediaType.APPLICATION_JSON
        )).isTrue();
        assertThat(converter.canWrite(
            Object.class,
            CreateServiceRequestDTO.class,
            MediaType.APPLICATION_JSON
        )).isTrue();
        assertThat(converter.canWrite(CaseAssignmentUserRolesRequest.class, MediaType.APPLICATION_JSON)).isTrue();
        assertThat(converter.canWrite(CaseDataContent.class, MediaType.APPLICATION_JSON)).isFalse();
        assertThat(converter.canWrite(
            CaseDataContent.class,
            CaseDataContent.class,
            MediaType.APPLICATION_JSON
        )).isFalse();
        assertThat(converter.canRead(CaseResource.class, MediaType.APPLICATION_JSON)).isFalse();
    }
}
