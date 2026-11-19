package uk.gov.hmcts.reform.pcs.jsoncontract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Shared support for the JSON contract tests: baseline files, Instancio settings and a whole-tree JSON
 * comparison that reports every differing path.
 *
 * <p>Uses only APIs that are identical on Spring Boot 3 and 4, so the tests run unchanged on both sides of
 * a framework upgrade.
 */
final class JsonContract {

    static final Path BASELINE_DIR = Path.of("src/integrationTest/resources/json-contract");

    /** Regenerate baselines only for an intentional model change, never to make an upgrade pass. */
    static final boolean UPDATE = Boolean.parseBoolean(System.getenv("JSON_CONTRACT_UPDATE"));

    /** Used only to read and pretty-print the baseline files; never to (de)serialise application data. */
    private static final ObjectMapper TREE_MAPPER = JsonMapper.builder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();

    private JsonContract() {
    }

    static Settings instancioSettings() {
        return Settings.create()
            .set(Keys.COLLECTION_MIN_SIZE, 1)
            .set(Keys.COLLECTION_MAX_SIZE, 2)
            .set(Keys.MAP_MIN_SIZE, 1)
            .set(Keys.MAP_MAX_SIZE, 1)
            .set(Keys.STRING_MIN_LENGTH, 3)
            .set(Keys.STRING_MAX_LENGTH, 10)
            .set(Keys.MAX_DEPTH, 10);
    }

    static JsonNode parse(String json) {
        try {
            return TREE_MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IllegalStateException("Could not parse JSON: " + abbreviate(json), e);
        }
    }

    static void writeBaseline(String name, JsonNode json) throws IOException {
        Path file = BASELINE_DIR.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, TREE_MAPPER.writeValueAsString(sortScalarArrays(json)) + System.lineSeparator());
    }

    /**
     * Sorts arrays of plain values (which include Java Sets with no stable order) so that regenerating a
     * baseline is byte-for-byte repeatable and its diff shows only real changes.
     */
    private static JsonNode sortScalarArrays(JsonNode node) {
        JsonNode copy = parse(node.toString());
        sortInPlace(copy);
        return copy;
    }

    private static void sortInPlace(JsonNode node) {
        if (node.isObject()) {
            node.forEach(JsonContract::sortInPlace);
        } else if (node.isArray()) {
            if (isScalarArray(node)) {
                List<JsonNode> values = new ArrayList<>();
                node.forEach(values::add);
                values.sort((a, b) -> a.toString().compareTo(b.toString()));
                ArrayNode array = (ArrayNode) node;
                array.removeAll();
                array.addAll(values);
            } else {
                node.forEach(JsonContract::sortInPlace);
            }
        }
    }

    static JsonNode readBaseline(String name) throws IOException {
        Path file = BASELINE_DIR.resolve(name);
        assertThat(file)
            .as("JSON contract baseline %s is missing; generate it with JSON_CONTRACT_UPDATE=true", file)
            .exists();
        return TREE_MAPPER.readTree(file.toFile());
    }

    static void assertSameTree(String what, JsonNode expected, JsonNode actual) {
        List<String> differences = new ArrayList<>();
        // Compare what goes over the wire: re-parse both sides from their JSON text so that equal values
        // held as different Java number types (int vs long) are not reported, while 1 vs 1.0 or "1" vs 1 are.
        diff("$", parse(expected.toString()), parse(actual.toString()), differences);
        if (!differences.isEmpty()) {
            int shown = Math.min(differences.size(), 50);
            fail(what + " differs from the JSON contract baseline in " + differences.size() + " place(s):\n  "
                + String.join("\n  ", differences.subList(0, shown))
                + (differences.size() > shown ? "\n  ..." : ""));
        }
    }

    private static void diff(String path, JsonNode expected, JsonNode actual, List<String> out) {
        if (expected == null || expected.isMissingNode()) {
            out.add(path + ": unexpected value " + abbreviate(String.valueOf(actual)));
            return;
        }
        if (actual == null || actual.isMissingNode()) {
            out.add(path + ": missing, expected " + abbreviate(String.valueOf(expected)));
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
                    out.add(path + ": " + abbreviate(actual.toString()) + ", expected "
                        + abbreviate(expected.toString()));
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
            out.add(path + ": " + abbreviate(actual.toString()) + ", expected " + abbreviate(expected.toString()));
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

    private static String abbreviate(String text) {
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }
}
