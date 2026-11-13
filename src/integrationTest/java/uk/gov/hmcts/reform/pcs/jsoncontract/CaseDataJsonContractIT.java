package uk.gov.hmcts.reform.pcs.jsoncontract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
 *
 * <p>This class deliberately uses only APIs that are identical on Spring Boot 3 and 4, so the same
 * file runs unchanged on both sides of the upgrade.
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PCSCase JSON contract")
class CaseDataJsonContractIT extends AbstractPostgresContainerIT {

    private static final long SEED = 9529L;
    private static final Path BASELINE_DIR = Path.of("src/integrationTest/resources/json-contract");
    private static final String PRIMARY_BASELINE = "pcs-case.primary-mapper.json";
    private static final String DRAFT_BASELINE = "pcs-case.draft-mapper.json";

    private static final boolean UPDATE = Boolean.parseBoolean(System.getenv("JSON_CONTRACT_UPDATE"));

    /** Used only to read and pretty-print the baseline files; never to (de)serialise case data. */
    private static final ObjectMapper TREE_MAPPER = JsonMapper.builder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();

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
        sample = fullyPopulatedCase();
        if (UPDATE) {
            writeBaseline(PRIMARY_BASELINE, primaryMapper.valueToTree(sample));
            writeBaseline(DRAFT_BASELINE, draftMapper.valueToTree(sample));
        }
    }

    @Test
    @DisplayName("primary mapper writes the same JSON as the baseline")
    void primaryMapperWritesBaseline() throws IOException {
        assertMatchesBaseline(PRIMARY_BASELINE, primaryMapper.valueToTree(sample));
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
        assertMatchesBaseline(DRAFT_BASELINE, draftMapper.valueToTree(sample));
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

        JsonNode written = TREE_MAPPER.readTree(output.getBodyAsString(StandardCharsets.UTF_8));
        assertSameTree("Spring MVC JSON converter", readBaseline(PRIMARY_BASELINE), written);
    }

    private static PCSCase fullyPopulatedCase() {
        Settings settings = Settings.create()
            .set(Keys.COLLECTION_MIN_SIZE, 1)
            .set(Keys.COLLECTION_MAX_SIZE, 2)
            .set(Keys.MAP_MIN_SIZE, 1)
            .set(Keys.MAP_MAX_SIZE, 1)
            .set(Keys.STRING_MIN_LENGTH, 3)
            .set(Keys.STRING_MAX_LENGTH, 10)
            .set(Keys.MAX_DEPTH, 10);

        return Instancio.of(PCSCase.class)
            .withSeed(SEED)
            .withSettings(settings)
            .create();
    }

    @SuppressWarnings("unchecked")
    private HttpMessageConverter<Object> jsonConverter() {
        return handlerAdapter.getMessageConverters().stream()
            .filter(c -> c.canWrite(PCSCase.class, MediaType.APPLICATION_JSON))
            .map(c -> (HttpMessageConverter<Object>) c)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No Spring MVC converter can write PCSCase as JSON"));
    }

    private void assertMatchesBaseline(String name, JsonNode actual) throws IOException {
        assertSameTree(name, readBaseline(name), actual);
    }

    private static void writeBaseline(String name, JsonNode json) throws IOException {
        Files.createDirectories(BASELINE_DIR);
        Files.writeString(BASELINE_DIR.resolve(name), TREE_MAPPER.writeValueAsString(json) + System.lineSeparator());
    }

    private static JsonNode readBaseline(String name) throws IOException {
        Path file = BASELINE_DIR.resolve(name);
        assertThat(file)
            .as("JSON contract baseline %s is missing; generate it with JSON_CONTRACT_UPDATE=true", file)
            .exists();
        return TREE_MAPPER.readTree(file.toFile());
    }

    private static void assertSameTree(String what, JsonNode expected, JsonNode actual) {
        List<String> differences = new ArrayList<>();
        // Compare what goes over the wire: re-parse both sides from their JSON text so that equal values
        // held as different Java number types (int vs long) are not reported, while 1 vs 1.0 or "1" vs 1 are.
        diff("$", reparse(expected), reparse(actual), differences);
        if (!differences.isEmpty()) {
            int shown = Math.min(differences.size(), 50);
            fail(what + " differs from the JSON contract baseline in " + differences.size() + " place(s):\n  "
                + String.join("\n  ", differences.subList(0, shown))
                + (differences.size() > shown ? "\n  ..." : ""));
        }
    }

    private static void diff(String path, JsonNode expected, JsonNode actual, List<String> out) {
        if (expected == null || expected.isMissingNode()) {
            out.add(path + ": unexpected value " + abbreviate(actual));
            return;
        }
        if (actual == null || actual.isMissingNode()) {
            out.add(path + ": missing, expected " + abbreviate(expected));
            return;
        }
        if (expected.isObject() && actual.isObject()) {
            TreeSet<String> names = new TreeSet<>();
            expected.fieldNames().forEachRemaining(names::add);
            actual.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                diff(path + "." + name, expected.get(name), actual.get(name), out);
            }
            return;
        }
        if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                out.add(path + ": array size " + actual.size() + ", expected " + expected.size());
                return;
            }
            if (isScalarArray(expected) && isScalarArray(actual)) {
                // Arrays of plain values include Java Sets (e.g. Set<SomeGround>) whose iteration order is not
                // stable between JVM runs, so compare their contents regardless of order.
                if (!sortedText(expected).equals(sortedText(actual))) {
                    out.add(path + ": " + abbreviate(actual) + ", expected " + abbreviate(expected));
                }
                return;
            }
            Iterator<JsonNode> e = expected.elements();
            Iterator<JsonNode> a = actual.elements();
            for (int i = 0; e.hasNext(); i++) {
                diff(path + "[" + i + "]", e.next(), a.next(), out);
            }
            return;
        }
        if (!expected.equals(actual)) {
            out.add(path + ": " + abbreviate(actual) + ", expected " + abbreviate(expected));
        }
    }

    private static boolean isScalarArray(JsonNode array) {
        for (JsonNode element : array) {
            if (element.isContainerNode()) {
                return false;
            }
        }
        return true;
    }

    private static List<String> sortedText(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(element -> values.add(element.toString()));
        values.sort(null);
        return values;
    }

    private static JsonNode reparse(JsonNode node) {
        try {
            return TREE_MAPPER.readTree(node.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not re-parse JSON", e);
        }
    }

    private static String abbreviate(JsonNode node) {
        String text = String.valueOf(node);
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }

}
