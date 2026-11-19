package uk.gov.hmcts.reform.pcs.ccd.definition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Collapses the per-role copies of each CCD tab in a generated definition into a single tab with no role.
 *
 * <p>The CCD SDK writes one {@code CaseTypeTab} per access profile named in {@code forRoles}, each with its own
 * TabID, so a user who holds two of those profiles sees the tab twice. CCD shows a tab with a blank role to anyone
 * who can read at least one of its fields, and the field permissions are generated from the same role lists, so the
 * collapsed tab reaches the same users as before. Tabs named in the keep set are left as role copies because other
 * profiles can read their fields.
 */
@Slf4j
public final class TabCollapser {

    static final Set<String> KEEP_ROLE_COPIES = Set.of("support");

    private static final String TAB_ID = "TabID";
    private static final String USER_ROLE = "UserRole";
    private static final String TAB_DISPLAY_ORDER = "TabDisplayOrder";
    private static final String CASE_TYPE_TAB_DIR = "CaseTypeTab";

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<Map<String, Object>>> ROWS = new TypeReference<>() {
    };

    private TabCollapser() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: TabCollapser <definitions root>");
        }
        collapse(Path.of(args[0]));
    }

    /**
     * Collapses every {@code <case type>/CaseTypeTab} folder under the definitions root. Support keeps its role copies.
     */
    public static void collapse(Path definitionsRoot) {
        collapse(definitionsRoot, KEEP_ROLE_COPIES);
    }

    static void collapse(Path definitionsRoot, Set<String> keepRoleCopies) {
        try (Stream<Path> caseTypes = Files.list(definitionsRoot)) {
            caseTypes.map(caseType -> caseType.resolve(CASE_TYPE_TAB_DIR))
                .filter(Files::isDirectory)
                .forEach(tabDir -> collapseTabFolder(tabDir, keepRoleCopies));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void collapseTabFolder(Path tabDir, Set<String> keepRoleCopies) {
        Map<String, List<TabCopy>> copiesByTab = new LinkedHashMap<>();
        for (Path file : listJsonFiles(tabDir)) {
            TabCopy copy = TabCopy.read(file);
            if (copy.role().isEmpty() || keepRoleCopies.contains(copy.baseTabId())) {
                continue;
            }
            copiesByTab.computeIfAbsent(copy.baseTabId(), id -> new ArrayList<>()).add(copy);
        }

        copiesByTab.forEach((baseTabId, copies) -> {
            if (copies.size() > 1) {
                collapseCopies(tabDir, baseTabId, copies);
            }
        });
    }

    private static void collapseCopies(Path tabDir, String baseTabId, List<TabCopy> copies) {
        TabCopy kept = copies.stream().min(Comparator.comparingInt(TabCopy::displayOrder)).orElseThrow();
        for (TabCopy other : copies) {
            if (!other.rowsIgnoringRoleAndOrder().equals(kept.rowsIgnoringRoleAndOrder())) {
                throw new IllegalStateException(
                    "Tab copies differ and cannot be collapsed: " + kept.file() + " vs " + other.file());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : kept.rows()) {
            Map<String, Object> collapsed = new LinkedHashMap<>(row);
            collapsed.put(TAB_ID, baseTabId);
            collapsed.put(USER_ROLE, "");
            rows.add(collapsed);
        }

        try {
            for (TabCopy copy : copies) {
                Files.delete(copy.file());
            }
            Path output = tabDir.resolve(kept.displayOrder() + "_" + baseTabId + ".json");
            MAPPER.writeValue(output.toFile(), rows);
            log.info("Collapsed {} copies of tab {} into {}", copies.size(), baseTabId, output.getFileName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> listJsonFiles(Path tabDir) {
        try (Stream<Path> files = Files.list(tabDir)) {
            return files.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * One generated tab file: a set of rows sharing a TabID, with the role on the single row whose
     * TabFieldDisplayOrder is 1. Rows are sorted by CaseFieldID, so that row is not necessarily the first.
     */
    record TabCopy(Path file, List<Map<String, Object>> rows, String role, String baseTabId, int displayOrder) {

        static TabCopy read(Path file) {
            List<Map<String, Object>> rows;
            try {
                rows = MAPPER.readValue(file.toFile(), ROWS);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (rows.isEmpty()) {
                throw new IllegalStateException("Empty tab file: " + file);
            }

            List<String> roles = rows.stream()
                .map(row -> stringValue(row.get(USER_ROLE)))
                .filter(role -> !role.isEmpty())
                .toList();
            if (roles.size() > 1) {
                throw new IllegalStateException("More than one role row in tab file: " + file);
            }
            String role = roles.isEmpty() ? "" : roles.get(0);

            String tabId = stringValue(rows.get(0).get(TAB_ID));
            if (!tabId.endsWith(role)) {
                throw new IllegalStateException(
                    "TabID '" + tabId + "' does not end with its role '" + role + "' in " + file);
            }
            String baseTabId = tabId.substring(0, tabId.length() - role.length());

            int displayOrder = ((Number) rows.get(0).get(TAB_DISPLAY_ORDER)).intValue();
            return new TabCopy(file, rows, role, baseTabId, displayOrder);
        }

        List<Map<String, Object>> rowsIgnoringRoleAndOrder() {
            List<Map<String, Object>> stripped = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> copy = new LinkedHashMap<>(row);
                copy.remove(TAB_ID);
                copy.remove(USER_ROLE);
                copy.remove(TAB_DISPLAY_ORDER);
                stripped.add(copy);
            }
            return stripped;
        }

        private static String stringValue(Object value) {
            return value == null ? "" : value.toString();
        }
    }
}
