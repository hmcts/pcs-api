package uk.gov.hmcts.reform.pcs.ccd.definition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabCollapserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> ROWS = new TypeReference<>() {
    };

    @TempDir
    private Path definitions;

    private Path tabDir;

    @BeforeEach
    void setUp() throws IOException {
        tabDir = definitions.resolve("PCS-123").resolve("CaseTypeTab");
        Files.createDirectories(tabDir);
    }

    @Test
    void shouldCollapseRoleCopiesIntoOneTabWithNoRole() throws IOException {
        // Rows are sorted by CaseFieldID, so the role row ("summaryMarkdownLabel") is not first
        writeTab("summary", "citizen", 18, List.of(
            row("Claimant details", 3, "summaryTab_ClaimantDetails!=\"\""),
            row("summaryMarkdownLabel", 1, null),
            row("summaryTab_ClaimantDetails", 2, null)
        ));
        writeTab("summary", "claimant", 100, List.of(
            row("Claimant details", 3, "summaryTab_ClaimantDetails!=\"\""),
            row("summaryMarkdownLabel", 1, null),
            row("summaryTab_ClaimantDetails", 2, null)
        ));
        writeTab("summary", "[DEFENDANT]", 19, List.of(
            row("Claimant details", 3, "summaryTab_ClaimantDetails!=\"\""),
            row("summaryMarkdownLabel", 1, null),
            row("summaryTab_ClaimantDetails", 2, null)
        ));

        TabCollapser.collapse(definitions, Set.of());

        assertThat(files()).containsExactly("18_summary.json");
        List<Map<String, Object>> rows = read("18_summary.json");
        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(row -> row.get("TabID")).containsOnly("summary");
        assertThat(rows).extracting(row -> row.get("UserRole")).containsOnly("");
        assertThat(rows).extracting(row -> row.get("TabDisplayOrder")).containsOnly(18);
        assertThat(rows).extracting(row -> row.get("CaseFieldID"))
            .containsExactly("Claimant details", "summaryMarkdownLabel", "summaryTab_ClaimantDetails");
        assertThat(rows.get(0)).containsEntry("FieldShowCondition", "summaryTab_ClaimantDetails!=\"\"");
        assertThat(rows.get(1)).containsEntry("TabShowCondition", "[STATE]!=\"AWAITING_SUBMISSION_TO_HMCTS\"");
    }

    @Test
    void shouldKeepTheCopyWithTheLowestNumericDisplayOrder() throws IOException {
        writeTab("caseDetails", "citizen", 100, List.of(row("Case details", 1, null)));
        writeTab("caseDetails", "claimant", 18, List.of(row("Case details", 1, null)));

        TabCollapser.collapse(definitions, Set.of());

        assertThat(files()).containsExactly("18_caseDetails.json");
    }

    @Test
    void shouldLeaveTabsInTheKeepSetAsRoleCopies() throws IOException {
        writeTab("support", "citizen", 12, List.of(row("flagLauncherExternal", 1, null)));
        writeTab("support", "[DEFENDANT]", 13, List.of(row("flagLauncherExternal", 1, null)));
        writeTab("support", "defendant-solicitor", 14, List.of(row("flagLauncherExternal", 1, null)));

        TabCollapser.collapse(definitions, Set.of("support"));

        assertThat(files()).containsExactly(
            "12_supportcitizen.json", "13_support[DEFENDANT].json", "14_supportdefendant-solicitor.json");
    }

    @Test
    void shouldLeaveTabsWithNoRoleAndSingleCopiesAlone() throws IOException {
        writeTab("nextSteps", "", 1,
                 List.of(row("nextStepsMarkdown", 2, null), row("nextStepsMarkdownLabel", 1, null)));
        writeTab("onlyOne", "judge", 2, List.of(row("judicialNotesMarkdown", 1, null)));
        Path before = tabDir.resolve("1_nextSteps.json");
        byte[] nextSteps = Files.readAllBytes(before);

        TabCollapser.collapse(definitions, Set.of());

        assertThat(files()).containsExactly("1_nextSteps.json", "2_onlyOnejudge.json");
        assertThat(Files.readAllBytes(before)).isEqualTo(nextSteps);
    }

    @Test
    void shouldFailWhenCopiesDiffer() throws IOException {
        writeTab("summary", "citizen", 18, List.of(row("summaryMarkdownLabel", 1, null)));
        writeTab("summary", "claimant", 19, List.of(row("summaryMarkdownLabel", 1, "someField=\"Yes\"")));

        assertThatThrownBy(() -> TabCollapser.collapse(definitions, Set.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("differ");
    }

    @Test
    void shouldFailWhenTabIdDoesNotEndWithItsRole() throws IOException {
        List<Map<String, Object>> rows = List.of(row("field", 1, null));
        rows.get(0).put("TabID", "summaryX");
        rows.get(0).put("UserRole", "citizen");
        MAPPER.writeValue(tabDir.resolve("5_summaryX.json").toFile(), rows);
        writeTab("summary", "claimant", 6, List.of(row("field", 1, null)));

        assertThatThrownBy(() -> TabCollapser.collapse(definitions, Set.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does not end with its role");
    }

    @Test
    void shouldIgnoreCaseTypesWithoutATabFolder() throws IOException {
        Files.createDirectories(definitions.resolve("OTHER"));
        writeTab("caseDetails", "citizen", 1, List.of(row("Case details", 1, null)));
        writeTab("caseDetails", "claimant", 2, List.of(row("Case details", 1, null)));

        TabCollapser.collapse(definitions, Set.of());

        assertThat(files()).containsExactly("1_caseDetails.json");
    }

    private void writeTab(String tabId, String role, int displayOrder, List<Map<String, Object>> fields)
        throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            Map<String, Object> row = new LinkedHashMap<>(field);
            row.put("TabID", tabId + role);
            boolean firstField = Integer.valueOf(1).equals(row.get("TabFieldDisplayOrder"));
            row.put("UserRole", firstField ? role : "");
            row.put("TabDisplayOrder", displayOrder);
            if (firstField) {
                row.put("TabShowCondition", "[STATE]!=\"AWAITING_SUBMISSION_TO_HMCTS\"");
            }
            rows.add(row);
        }
        MAPPER.writeValue(tabDir.resolve(displayOrder + "_" + tabId + role + ".json").toFile(), rows);
    }

    private static Map<String, Object> row(String caseFieldId, int tabFieldDisplayOrder, String showCondition) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("LiveFrom", "01/01/2017");
        row.put("CaseTypeID", "PCS-123");
        row.put("Channel", "CaseWorker");
        row.put("TabLabel", "Label");
        row.put("TabFieldDisplayOrder", tabFieldDisplayOrder);
        row.put("CaseFieldID", caseFieldId);
        if (showCondition != null) {
            row.put("FieldShowCondition", showCondition);
        }
        return row;
    }

    private List<String> files() throws IOException {
        try (Stream<Path> files = Files.list(tabDir)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }

    private List<Map<String, Object>> read(String name) throws IOException {
        return MAPPER.readValue(tabDir.resolve(name).toFile(), ROWS);
    }
}
