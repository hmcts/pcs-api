package uk.gov.hmcts.reform.pcs.jsoncontract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.assertSameTree;
import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.parse;
import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.readBaseline;
import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.writeBaseline;

/**
 * Pins the JSON wire and storage contract of {@link PCSCase} so that framework upgrades (Spring Boot,
 * Jackson, CCD SDK) cannot silently change or lose case data.
 *
 * <p>A fully populated case (fixed Instancio seed, so every one of the {@code @JsonUnwrapped} sections
 * and their nested complex types carry values) is written through the application's own mapper beans
 * and compared, as a whole JSON tree, with the committed baselines under
 * {@code src/integrationTest/resources/json-contract/}. The baselines are then read back and written
 * again, which catches values that serialise correctly but are dropped on deserialisation
 * (e.g. jackson-databind #3178 with prefixed {@code @JsonUnwrapped}).
 *
 * <p>The baselines were generated on Spring Boot 3. If one of these tests fails after a framework or
 * library upgrade, the upgrade has changed the data pcs-api sends to CCD or stores in drafts: fix the
 * upgrade, do not regenerate the baseline. Regenerate only for an intentional model change, by running
 * the suite with {@code JSON_CONTRACT_UPDATE=true} and reviewing the diff.
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PCSCase JSON contract")
class CaseDataJsonContractIT extends AbstractPostgresContainerIT {

    private static final long SEED = 9529L;
    private static final String PRIMARY_BASELINE = "pcs-case.primary-mapper.json";
    private static final String DRAFT_BASELINE = "pcs-case.draft-mapper.json";

    @Autowired
    private ObjectMapper primaryMapper;

    @Autowired
    @Qualifier("draftCaseDataObjectMapper")
    private ObjectMapper draftMapper;

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    private PCSCase sample;

    @BeforeAll
    void createSample() throws IOException {
        sample = Instancio.of(PCSCase.class)
            .withSeed(SEED)
            .withSettings(JsonContract.instancioSettings())
            .create();
        if (JsonContract.UPDATE) {
            writeBaseline(PRIMARY_BASELINE, primaryMapper.valueToTree(sample));
            writeBaseline(DRAFT_BASELINE, draftMapper.valueToTree(sample));
        }
    }

    @Test
    @DisplayName("primary mapper writes the same JSON as the baseline")
    void primaryMapperWritesBaseline() throws IOException {
        assertSameTree(PRIMARY_BASELINE, readBaseline(PRIMARY_BASELINE), primaryMapper.valueToTree(sample));
    }

    @Test
    @DisplayName("primary mapper reads the baseline back without losing any value")
    void primaryMapperRoundTripsBaseline() throws IOException {
        JsonNode baseline = readBaseline(PRIMARY_BASELINE);
        PCSCase read = primaryMapper.treeToValue(baseline, PCSCase.class);

        assertSameTree("primary mapper JSON -> PCSCase -> JSON", baseline, primaryMapper.valueToTree(read));
    }

    @Test
    @DisplayName("draft mapper writes the same JSON as the baseline")
    void draftMapperWritesBaseline() throws IOException {
        assertSameTree(DRAFT_BASELINE, readBaseline(DRAFT_BASELINE), draftMapper.valueToTree(sample));
    }

    @Test
    @DisplayName("draft mapper reads the baseline back without losing any value")
    void draftMapperRoundTripsBaseline() throws IOException {
        JsonNode baseline = readBaseline(DRAFT_BASELINE);
        PCSCase read = draftMapper.treeToValue(baseline, PCSCase.class);

        assertSameTree("draft mapper JSON -> PCSCase -> JSON", baseline, draftMapper.valueToTree(read));
    }

    @Test
    @DisplayName("Spring MVC writes case data exactly as the primary mapper does")
    void httpConverterWritesBaseline() throws IOException {
        HttpMessageConverter<Object> converter = jsonConverter();
        MockHttpOutputMessage output = new MockHttpOutputMessage();

        converter.write(sample, MediaType.APPLICATION_JSON, output);

        JsonNode written = parse(output.getBodyAsString(StandardCharsets.UTF_8));
        assertSameTree("Spring MVC JSON converter", readBaseline(PRIMARY_BASELINE), written);
    }

    @SuppressWarnings("unchecked")
    private HttpMessageConverter<Object> jsonConverter() {
        return handlerAdapter.getMessageConverters().stream()
            .filter(c -> c.canWrite(PCSCase.class, MediaType.APPLICATION_JSON))
            .map(c -> (HttpMessageConverter<Object>) c)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No Spring MVC converter can write PCSCase as JSON"));
    }
}
