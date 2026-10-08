package uk.gov.hmcts.reform.pcs.data.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class HearingReferenceDataMigrationTest {

    private static final String REFERENCE_TABLES_SCRIPT = "db/migration/V033__create_hearing_reference_data_tables.sql";
    private static final String KEY_COLUMNS_SCRIPT = "db/migration/V034__add_hearing_reference_data_keys.sql";

    // Active PCS (AAA3) and shared rows in List of Values V76; V76 has no additional_roles values.
    private static final Map<String, Integer> EXPECTED_ROW_COUNTS = Map.ofEntries(
        Map.entry("judge_type", 9),
        Map.entry("hearing_channel", 5),
        Map.entry("hearing_priority", 2),
        Map.entry("hearing_case_subtype", 15),
        Map.entry("facilities", 16),
        Map.entry("auto_list_change_reasons", 2),
        Map.entry("additional_roles", 0),
        Map.entry("actual_part_heard_reason", 18),
        Map.entry("change_reasons", 16),
        Map.entry("listing_status", 4),
        Map.entry("actual_cancellation_reason", 14),
        Map.entry("case_management_cancellation_reason", 27),
        Map.entry("hearing_type", 12),
        Map.entry("sign_language", 12),
        Map.entry("unavailable_type", 3),
        Map.entry("hearing_case_type", 15),
        Map.entry("interpreter_language", 218),
        Map.entry("hearing_subchannel", 11),
        Map.entry("custody_status", 3),
        Map.entry("party_relationship_type", 2)
    );

    private static final Pattern CREATE_TABLE = Pattern.compile("CREATE TABLE public\\.(\\w+)");
    private static final Pattern INSERT_BLOCK = Pattern.compile(
        "INSERT INTO public\\.(\\w+) \\([^)]*\\)\\s+VALUES(.*?);", Pattern.DOTALL);
    private static final Pattern ROW_KEY = Pattern.compile("^\\s*\\('((?:[^']|'')*)'", Pattern.MULTILINE);
    private static final Pattern HEARING_KEY_COLUMN = Pattern.compile(
        "ADD COLUMN (\\w+_key) VARCHAR\\(64\\) CONSTRAINT \\w+ REFERENCES public\\.(\\w+) \\(key\\)");

    @Test
    void shouldCreateEveryHearingReferenceTable() throws IOException {
        List<String> createdTables = new ArrayList<>();
        Matcher matcher = CREATE_TABLE.matcher(readScript(REFERENCE_TABLES_SCRIPT));
        while (matcher.find()) {
            createdTables.add(matcher.group(1));
        }

        assertThat(createdTables).containsExactlyInAnyOrderElementsOf(EXPECTED_ROW_COUNTS.keySet());
    }

    @Test
    void shouldLoadMrdKeysWithoutDuplicates() throws IOException {
        Map<String, List<String>> keysByTable = new HashMap<>();
        Matcher block = INSERT_BLOCK.matcher(readScript(REFERENCE_TABLES_SCRIPT));
        while (block.find()) {
            List<String> keys = new ArrayList<>();
            Matcher key = ROW_KEY.matcher(block.group(2));
            while (key.find()) {
                keys.add(key.group(1));
            }
            keysByTable.put(block.group(1), keys);
        }

        EXPECTED_ROW_COUNTS.forEach((table, expectedCount) -> {
            List<String> keys = keysByTable.getOrDefault(table, List.of());
            assertThat(keys).as("keys loaded into %s", table).hasSize(expectedCount);
            assertThat(new HashSet<>(keys)).as("distinct keys in %s", table).hasSameSizeAs(keys);
        });
    }

    @Test
    void shouldLoadKnownMrdKeysNotDisplayValues() throws IOException {
        String script = readScript(REFERENCE_TABLES_SCRIPT);

        assertThat(script)
            .contains("('AAA3-FPH', 'First Possession Hearing'")
            .contains("('VID', 'Video', 'Fideo'")
            .contains("('INTER', 'In Person', 'Yn Bersonol'");
    }

    @Test
    void shouldReferenceReferenceTablesFromHearingKeyColumns() throws IOException {
        Map<String, String> referencedTableByColumn = new HashMap<>();
        Matcher matcher = HEARING_KEY_COLUMN.matcher(readScript(KEY_COLUMNS_SCRIPT));
        while (matcher.find()) {
            referencedTableByColumn.put(matcher.group(1), matcher.group(2));
        }

        assertThat(referencedTableByColumn).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
            Map.entry("hearing_type_key", "hearing_type"),
            Map.entry("hearing_case_type_key", "hearing_case_type"),
            Map.entry("hearing_case_subtype_key", "hearing_case_subtype"),
            Map.entry("hearing_priority_key", "hearing_priority"),
            Map.entry("hearing_channel_key", "hearing_channel"),
            Map.entry("hearing_subchannel_key", "hearing_subchannel"),
            Map.entry("listing_status_key", "listing_status"),
            Map.entry("change_reason_key", "change_reasons"),
            Map.entry("auto_list_change_reasons_key", "auto_list_change_reasons"),
            Map.entry("case_management_cancellation_reason_key", "case_management_cancellation_reason"),
            Map.entry("actual_cancellation_reason_key", "actual_cancellation_reason"),
            Map.entry("actual_part_heard_reason_key", "actual_part_heard_reason")
        ));
        assertThat(EXPECTED_ROW_COUNTS.keySet()).containsAll(referencedTableByColumn.values());
    }

    @Test
    void shouldMapEveryHearingKeyColumnOnHearingEntity() throws IOException {
        Set<String> entityFields = new HashSet<>();
        for (var field : HearingEntity.class.getDeclaredFields()) {
            entityFields.add(field.getName());
        }

        Matcher matcher = HEARING_KEY_COLUMN.matcher(readScript(KEY_COLUMNS_SCRIPT));
        while (matcher.find()) {
            assertThat(entityFields).as("HearingEntity field for column " + matcher.group(1))
                .contains(toCamelCase(matcher.group(1)));
        }
    }

    private static String toCamelCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else {
                result.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            }
        }
        return result.toString();
    }

    private static String readScript(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
