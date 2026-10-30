package uk.gov.hmcts.reform.pcs.ccd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the definition written by {@code generateCCDConfig} and pins what HDPI-9534 relies on:
 * one tab per topic, and field permissions deciding who sees it. Runs from the {@code definitionTest} task.
 */
@Tag("ccd-definition")
class GeneratedDefinitionTest {

    private static final Path DEFINITION_DIR = Path.of("build/definitions/PCS");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> INTERNAL_PROFILES = Set.of(
        "judge", "leadership-judge", "circuit-judge", "fee-paid-judge",
        "ctsc", "hearing-centre-admin", "wlu-admin"
    );
    private static final Set<String> PARTY_VISIBLE_PROFILES = union(INTERNAL_PROFILES, Set.of(
        "citizen", "[DEFENDANT]", "defendant-solicitor",
        "caseworker-pcs-solicitor", "claimant", "claimant-solicitor"
    ));
    private static final Set<String> JUDICIAL_PROFILES = Set.of(
        "judge", "leadership-judge", "circuit-judge", "fee-paid-judge"
    );
    private static final Set<String> DEFENDANT_PROFILES = Set.of(
        "[DEFENDANT]", "defendant-solicitor", "[DEFENDANTSOLICITOR]"
    );
    private static final Set<String> DOCUMENT_PROFILES = union(PARTY_VISIBLE_PROFILES, Set.of(
        "[CLAIMANTSOLICITOR]", "[DEFENDANTSOLICITOR]", "pcs-system-update"
    ));

    /** field id -> profiles with R. */
    private static Map<String, Set<String>> readers;
    /** tab id -> rows of that tab file. */
    private static Map<String, List<JsonNode>> tabs;

    @BeforeAll
    static void loadDefinition() throws IOException {
        assertThat(DEFINITION_DIR).as("run generateCCDConfig first").isDirectory();
        readers = new HashMap<>();
        for (JsonNode row : rows(DEFINITION_DIR.resolve("AuthorisationCaseField"))) {
            if (row.get("CRUD").asText().contains("R")) {
                readers.computeIfAbsent(row.get("CaseFieldID").asText(), k -> new HashSet<>())
                    .add(row.get("UserRole").asText());
            }
        }
        tabs = new TreeMap<>();
        try (Stream<Path> files = Files.list(DEFINITION_DIR.resolve("CaseTypeTab"))) {
            files.sorted().forEach(file -> {
                List<JsonNode> tabRows = rows(file);
                tabs.put(tabRows.getFirst().get("TabID").asText(), tabRows);
            });
        }
    }

    @Test
    void everyTabExceptSupportIsWrittenOnceWithNoRole() {
        Map<String, List<String>> roleByTab = new TreeMap<>();
        tabs.forEach((tabId, tabRows) -> roleByTab.put(tabId, tabRows.stream()
            .map(row -> row.path("UserRole").asText(""))
            .filter(role -> !role.isEmpty())
            .distinct()
            .toList()));

        assertThat(roleByTab).hasSize(16);
        roleByTab.forEach((tabId, roles) -> {
            if (tabId.startsWith("support")) {
                assertThat(roles).as(tabId).hasSize(1);
            } else {
                assertThat(roles).as(tabId).isEmpty();
            }
        });
    }

    @Test
    void noProfilePairSeesTheSameTabTwiceExceptSupport() {
        List<String> profiles = readers.values().stream().flatMap(Set::stream).distinct().sorted().toList();
        List<String> duplicates = new ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            for (int j = i + 1; j < profiles.size(); j++) {
                Set<String> held = Set.of(profiles.get(i), profiles.get(j));
                Map<String, Integer> seen = new HashMap<>();
                tabs.values().forEach(tabRows -> {
                    if (visible(tabRows, held)) {
                        seen.merge(tabRows.getFirst().get("TabLabel").asText(), 1, Integer::sum);
                    }
                });
                seen.forEach((label, count) -> {
                    if (count > 1 && !label.equals("Support")) {
                        duplicates.add(label + " for " + held);
                    }
                });
            }
        }
        assertThat(duplicates).isEmpty();
    }

    @Test
    void historyStaysInternal() {
        assertThat(readers.get("caseHistory")).containsExactlyInAnyOrderElementsOf(INTERNAL_PROFILES);
    }

    @Test
    void historyTabHoldsOnlyTheHistoryViewer() {
        assertThat(tabs.get("CaseHistory")).extracting(row -> row.get("CaseFieldID").asText())
            .containsExactly("caseHistory");
    }

    /**
     * A field with no declared access on a role-less tab is readable by every profile and would surface
     * the tab to all of them. Only these four are meant to be.
     */
    @Test
    void onlyKnownFieldsOnRoleLessTabsAreReadableByEveryProfile() {
        // featureFlags declares no access, so its readers are exactly the profiles the SDK grants to undeclared fields.
        Set<String> everyProfile = readers.get("featureFlags");
        assertThat(everyProfile).hasSizeGreaterThan(PARTY_VISIBLE_PROFILES.size());
        List<String> everyoneReadable = new ArrayList<>();
        tabs.forEach((tabId, tabRows) -> {
            boolean roleLess = tabRows.stream().allMatch(row -> row.path("UserRole").asText("").isEmpty());
            if (roleLess) {
                tabRows.stream()
                    .map(row -> row.get("CaseFieldID").asText())
                    .filter(fieldId -> readers.getOrDefault(fieldId, Set.of()).containsAll(everyProfile))
                    .forEach(fieldId -> everyoneReadable.add(tabId + "/" + fieldId));
            }
        });
        assertThat(everyoneReadable).containsExactlyInAnyOrder(
            "nextSteps/nextStepsMarkdown", "nextSteps/nextStepsMarkdownLabel",
            "hidden/caseTitleMarkdown", "hidden/featureFlags"
        );
    }

    @Test
    void headingsCarryTheAccessOfTheSectionTheyHead() {
        assertThat(readers.get("Case details")).containsExactlyInAnyOrderElementsOf(PARTY_VISIBLE_PROFILES);
        assertThat(readers.get("Claimant details")).containsExactlyInAnyOrderElementsOf(PARTY_VISIBLE_PROFILES);
        assertThat(readers.get("judicialNotesMarkdownLabel")).containsExactlyInAnyOrderElementsOf(JUDICIAL_PROFILES);
        assertThat(readers.get("caseReviewDatesMarkdownLabel")).containsExactlyInAnyOrderElementsOf(INTERNAL_PROFILES);
    }

    @Test
    void declaredFieldAccessIsNotWidenedByRoleLessTabs() {
        assertThat(readers.get("dashboardData")).containsExactlyInAnyOrderElementsOf(DEFENDANT_PROFILES);
        assertThat(readers.get("caseFileView")).containsExactlyInAnyOrderElementsOf(DOCUMENT_PROFILES);
    }

    /** data-store rule: tab role blank or held, and at least one field the user can read. */
    private static boolean visible(List<JsonNode> tabRows, Set<String> held) {
        String role = tabRows.stream()
            .map(row -> row.path("UserRole").asText(""))
            .filter(r -> !r.isEmpty())
            .findFirst()
            .orElse("");
        if (!role.isEmpty() && !held.contains(role)) {
            return false;
        }
        return tabRows.stream()
            .map(row -> readers.getOrDefault(row.get("CaseFieldID").asText(), Set.of()))
            .anyMatch(fieldReaders -> fieldReaders.stream().anyMatch(held::contains));
    }

    private static List<JsonNode> rows(Path fileOrDir) {
        try {
            if (Files.isDirectory(fileOrDir)) {
                try (Stream<Path> files = Files.list(fileOrDir)) {
                    return files.sorted().flatMap(file -> rows(file).stream()).toList();
                }
            }
            List<JsonNode> out = new ArrayList<>();
            MAPPER.readTree(fileOrDir.toFile()).forEach(out::add);
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> out = new HashSet<>(a);
        out.addAll(b);
        return Set.copyOf(out);
    }
}
