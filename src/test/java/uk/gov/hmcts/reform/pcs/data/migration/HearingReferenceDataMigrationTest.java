package uk.gov.hmcts.reform.pcs.data.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class HearingReferenceDataMigrationTest {

    private static final String REFERENCE_TABLES_SCRIPT = "db/migration/V038__create_hearing_reference_data_tables.sql";
    private static final String KEY_COLUMNS_SCRIPT = "db/migration/V039__add_hearing_reference_data_keys.sql";

    private static final String KEY = "key VARCHAR(64) NOT NULL PK=%s";
    private static final String VALUE_EN = "value_en VARCHAR(128)";
    private static final String VALUE_CY = "value_cy VARCHAR(128)";
    private static final String LOV_ORDER = "lov_order INTEGER";
    private static final String ACTIVE = "active YES_NO";
    private static final String CREATED = "created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT";
    private static final String MODIFIED = "last_modified_date TIMESTAMP WITHOUT TIME ZONE";

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

    private static final Map<String, List<String>> EXPECTED_REFERENCE_TABLES = expectedReferenceTables();

    private static final Map<String, List<String>> EXPECTED_ENTITY_TABLES = Map.of(
        "hearing_venue", List.of(
            "id INTEGER NOT NULL PK=hearing_venue_pk IDENTITY",
            "case_hearing_id INTEGER NOT NULL"),
        "hearing_judge", List.of(
            "id INTEGER NOT NULL PK=hearing_attendees_pk IDENTITY",
            "case_hearing_id INTEGER NOT NULL FK=hearing_attendees_case_hearing_fk->hearing(id)",
            "judge_type_key VARCHAR(64) NOT NULL FK=hearing_judge_judge_type_fk->judge_type(key)",
            "additional_roles_key VARCHAR(64) NOT NULL "
                + "FK=hearing_judge_additional_roles_fk->additional_roles(key)"),
        "hearing_party", List.of(
            "id INTEGER NOT NULL PK=hearing_party_pk IDENTITY",
            "case_hearing_id INTEGER NOT NULL FK=hearing_party_case_hearing_fk->hearing(id)",
            "party_relationship_type_key VARCHAR(64) NOT NULL "
                + "FK=hearing_party_party_relationship_type_fk->party_relationship_type(key)",
            "custody_status_key VARCHAR(64) NOT NULL FK=hearing_party_custody_status_fk->custody_status(key)",
            "sign_language_key VARCHAR(64) NOT NULL FK=hearing_party_sign_language_fk->sign_language(key)",
            "unavailable_type_key VARCHAR(64) NOT NULL "
                + "FK=hearing_party_unavailable_type_fk->unavailable_type(key)",
            "interpreter_language_key VARCHAR(64) NOT NULL "
                + "FK=hearing_party_interpreter_language_fk->interpreter_language(key)")
    );

    private static final List<String> EXPECTED_HEARING_ADDED_COLUMNS = List.of(
        "date TIMESTAMP WITHOUT TIME ZONE",
        "notice_issued YES_NO",
        "notice_parties UUID",
        "pcs_case_id UUID FK=case_hearing_pcs_case_fk->pcs_case(id)",
        "listing_status HEARING_LISTING_STATUS",
        "created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT",
        "last_modified_date TIMESTAMP WITHOUT TIME ZONE",
        "hearing_venue_id INTEGER FK=case_hearing_hearing_venue_fk->hearing_venue(id)",
        "hearing_type_key VARCHAR(64) FK=case_hearing_hearing_type_fk->hearing_type(key)",
        "hearing_case_type_key VARCHAR(64) FK=case_hearing_hearing_case_type_fk->hearing_case_type(key)",
        "hearing_case_subtype_key VARCHAR(64) "
            + "FK=case_hearing_hearing_case_subtype_fk->hearing_case_subtype(key)",
        "hearing_priority_key VARCHAR(64) FK=case_hearing_hearing_priority_fk->hearing_priority(key)",
        "hearing_channel_key VARCHAR(64) FK=case_hearing_hearing_channel_fk->hearing_channel(key)",
        "hearing_subchannel_key VARCHAR(64) FK=case_hearing_hearing_subchannel_fk->hearing_subchannel(key)",
        "listing_status_key VARCHAR(64) FK=case_hearing_listing_status_fk->listing_status(key)",
        "change_reason_key VARCHAR(64) FK=case_hearing_change_reason_fk->change_reasons(key)",
        "auto_list_change_reasons_key VARCHAR(64) "
            + "FK=case_hearing_auto_list_change_reasons_fk->auto_list_change_reasons(key)",
        "case_management_cancellation_reason_key VARCHAR(64) "
            + "FK=case_hearing_case_management_cancellation_reason_fk->case_management_cancellation_reason(key)",
        "actual_cancellation_reason_key VARCHAR(64) "
            + "FK=case_hearing_actual_cancellation_reason_fk->actual_cancellation_reason(key)",
        "actual_part_heard_reason_key VARCHAR(64) "
            + "FK=case_hearing_actual_part_heard_reason_fk->actual_part_heard_reason(key)"
    );

    private static final Set<String> LEGACY_HEARING_COLUMNS = Set.of(
        "id", "case_id", "type", "other_hearing_type", "notice_wording", "hearing_date", "duration_hours",
        "duration_minutes", "notes", "issue_notice", "is_without_notice", "additional_information", "cancelled",
        "cancellation_reason", "duration_days"
    );

    private static final Set<String> MODEL_HEARING_COLUMNS = Set.of(
        "id", "other_hearing_type", "notice_wording", "date", "duration_hours", "duration_minutes", "notes",
        "notice_issued", "is_without_notice", "additional_information", "listing_status", "created_date",
        "last_modified_date", "pcs_case_id", "hearing_venue_id", "hearing_subchannel_key",
        "case_management_cancellation_reason_key", "actual_cancellation_reason_key", "hearing_case_subtype_key",
        "hearing_case_type_key", "hearing_type_key", "hearing_priority_key", "hearing_channel_key",
        "change_reason_key", "actual_part_heard_reason_key", "auto_list_change_reasons_key", "listing_status_key"
    );

    private static final Pattern CREATE_TABLE = Pattern.compile(
        "CREATE TABLE public\\.(\\w+)\\n\\((.*?)\\n\\);", Pattern.DOTALL);
    private static final Pattern ALTER_TABLE = Pattern.compile(
        "ALTER TABLE public\\.(\\w+)\\n(.*?);\\n", Pattern.DOTALL);
    private static final Pattern INSERT_BLOCK = Pattern.compile(
        "INSERT INTO public\\.(\\w+) \\([^)]*\\)\\s+VALUES(.*?);", Pattern.DOTALL);
    private static final Pattern ROW_KEY = Pattern.compile("^\\s*\\('((?:[^']|'')*)'", Pattern.MULTILINE);
    private static final Pattern COLUMN = Pattern.compile(
        "^(\\w+) (VARCHAR\\(\\d+\\)|INTEGER|UUID|YES_NO|TIMESTAMP WITHOUT TIME ZONE|TIMESTAMP|public\\.\\w+)(.*)$");
    private static final Pattern PRIMARY_KEY = Pattern.compile("CONSTRAINT (\\w+) PRIMARY KEY");
    private static final Pattern FOREIGN_KEY = Pattern.compile(
        "CONSTRAINT (\\w+) REFERENCES public\\.(\\w+) \\((\\w+)\\)");
    private static final Pattern ADD_COLUMN = Pattern.compile("ADD COLUMN (\\w+)");
    private static final Pattern DROP_COLUMN = Pattern.compile("DROP COLUMN (\\w+)");
    private static final Pattern GENERATED = Pattern.compile("GENERATED ALWAYS AS \\((\\w+)\\) STORED");

    @Test
    void shouldCreateEveryHearingReferenceTableWithTheModelColumns() throws IOException {
        Map<String, List<String>> tables = createdTables(readScript(REFERENCE_TABLES_SCRIPT));

        assertThat(tables).containsOnlyKeys(EXPECTED_REFERENCE_TABLES.keySet());
        EXPECTED_REFERENCE_TABLES.forEach((table, columns) ->
            assertThat(tables.get(table)).as("columns of %s", table).containsExactlyElementsOf(columns));
    }

    @Test
    void shouldCreateHearingVenueJudgeAndPartyWithTheModelColumns() throws IOException {
        Map<String, List<String>> tables = createdTables(readScript(KEY_COLUMNS_SCRIPT));

        assertThat(tables).containsOnlyKeys(EXPECTED_ENTITY_TABLES.keySet());
        EXPECTED_ENTITY_TABLES.forEach((table, columns) ->
            assertThat(tables.get(table)).as("columns of %s", table).containsExactlyElementsOf(columns));
    }

    @Test
    void shouldAddTheModelColumnsToHearingAndFacilities() throws IOException {
        Map<String, List<String>> altered = alteredTables(readScript(KEY_COLUMNS_SCRIPT));

        assertThat(altered.get("hearing")).containsExactlyElementsOf(EXPECTED_HEARING_ADDED_COLUMNS);
        assertThat(altered.get("facilities")).containsExactly(
            "hearing_venue_id INTEGER FK=facilities_hearing_venue_fk->hearing_venue(id)");
    }

    @Test
    void shouldResolveEveryQaFindingWhileKeepingLegacyHearingColumns() throws IOException {
        Set<String> columns = hearingColumnsAfterMigrations();

        assertThat(columns)
            .containsAll(MODEL_HEARING_COLUMNS)
            .containsAll(LEGACY_HEARING_COLUMNS)
            .contains("date", "notice_issued", "listing_status", "created_date", "last_modified_date",
                      "hearing_venue_id", "hearing_date", "issue_notice", "cancelled");
        assertThat(columns).contains("notice_parties", "pcs_case_id");
        assertThat(readScript("db/migration/V024__create_hearing_notice_parties_table.sql"))
            .contains("hearing_id INTEGER NOT NULL REFERENCES public.hearing (id)")
            .contains("party_id UUID NOT NULL REFERENCES public.party (id)");
    }

    @Test
    void shouldBackfillAndRequireTheMirroredModelColumns() throws IOException {
        assertThat(readScript(KEY_COLUMNS_SCRIPT))
            .contains("SET date = hearing_date, notice_issued = issue_notice, pcs_case_id = case_id;")
            .contains("ALTER COLUMN date SET NOT NULL")
            .contains("ALTER COLUMN notice_issued SET NOT NULL")
            .doesNotContain("GENERATED ALWAYS");
    }

    @Test
    void shouldNotDropOrRenameExistingColumnsOrTables() throws IOException {
        String script = readScript(KEY_COLUMNS_SCRIPT);

        assertThat(readScript(REFERENCE_TABLES_SCRIPT)).doesNotContain("DROP ").doesNotContain("RENAME ");
        assertThat(script).doesNotContain("DROP ").doesNotContain("RENAME COLUMN").doesNotContain("RENAME TO");
        assertThat(script).contains("RENAME CONSTRAINT hearing_pkey TO case_hearing_pk");
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
    void shouldMapEveryPersistedHearingColumnOnHearingEntity() throws IOException {
        Set<String> entityFields = new HashSet<>();
        for (Field field : HearingEntity.class.getDeclaredFields()) {
            entityFields.add(field.getName());
        }

        for (String column : EXPECTED_HEARING_ADDED_COLUMNS) {
            String name = column.substring(0, column.indexOf(' '));
            assertThat(entityFields).as("HearingEntity field for column " + name).contains(toCamelCase(name));
        }
    }

    private static Map<String, List<String>> expectedReferenceTables() {
        Map<String, List<String>> tables = new LinkedHashMap<>();
        tables.put("judge_type", columns("judge_type_pk", VALUE_EN, VALUE_CY, LOV_ORDER, ACTIVE, CREATED, MODIFIED));
        tables.put("hearing_channel", columns("hearing_channel_pk", VALUE_EN, VALUE_CY, ACTIVE, CREATED, MODIFIED));
        tables.put("hearing_priority", columns("hearing_priority_pk", VALUE_EN, VALUE_CY, ACTIVE, CREATED, MODIFIED));
        tables.put("hearing_case_subtype", columns("hearing_case_subtype_pk", VALUE_EN,
            "parent_category VARCHAR(64)", "parent_key VARCHAR(64)", ACTIVE, CREATED, MODIFIED));
        tables.put("facilities", columns("facilities_pk", VALUE_EN, VALUE_CY, ACTIVE, CREATED, MODIFIED));
        tables.put("auto_list_change_reasons", columns("auto_list_change_reasons_pk", VALUE_EN, ACTIVE));
        tables.put("additional_roles", columns("additional_roles_pk", VALUE_EN, ACTIVE));
        tables.put("actual_part_heard_reason", columns("part_heard_reason_pk", VALUE_EN, ACTIVE, CREATED, MODIFIED));
        tables.put("change_reasons", columns("change_reason_pk", VALUE_EN, ACTIVE, CREATED, MODIFIED));
        tables.put("listing_status", columns("listing_status_pk", VALUE_EN, ACTIVE));
        tables.put("actual_cancellation_reason",
                   columns("actual_cancellation_reason_pk", VALUE_EN, ACTIVE, CREATED, MODIFIED));
        tables.put("case_management_cancellation_reason",
                   columns("case_management_cancellation_reason_pk", VALUE_EN, ACTIVE, CREATED, MODIFIED));
        tables.put("hearing_type",
                   columns("hearing_type_pk", VALUE_EN, VALUE_CY, LOV_ORDER, ACTIVE, CREATED, MODIFIED));
        tables.put("sign_language", columns("sign_language_pk", VALUE_EN, VALUE_CY, ACTIVE));
        tables.put("unavailable_type", columns("unavailable_type_pk", VALUE_EN, VALUE_CY, ACTIVE));
        tables.put("hearing_case_type", columns("case_type_pk", VALUE_EN, ACTIVE, CREATED, MODIFIED));
        tables.put("interpreter_language", columns("interpreter_language_pk", VALUE_EN, VALUE_CY, ACTIVE));
        tables.put("hearing_subchannel", columns("hearing_subchannel_pk", VALUE_EN, VALUE_CY,
            "parentcategory VARCHAR(64)", "parentkey VARCHAR(64)", ACTIVE, CREATED, MODIFIED));
        tables.put("custody_status", columns("custody_status_pk", VALUE_EN, ACTIVE, CREATED, MODIFIED));
        tables.put("party_relationship_type",
                   columns("party_relationship_pk", VALUE_EN, VALUE_CY, ACTIVE, CREATED, MODIFIED));
        return tables;
    }

    private static List<String> columns(String primaryKeyName, String... others) {
        List<String> columns = new ArrayList<>();
        columns.add(KEY.formatted(primaryKeyName));
        columns.addAll(Arrays.asList(others));
        return columns;
    }

    private static Map<String, List<String>> createdTables(String script) {
        Map<String, List<String>> tables = new LinkedHashMap<>();
        Matcher matcher = CREATE_TABLE.matcher(script);
        while (matcher.find()) {
            tables.put(matcher.group(1), parseColumns(matcher.group(2).lines().toList()));
        }
        return tables;
    }

    private static Map<String, List<String>> alteredTables(String script) {
        Map<String, List<String>> tables = new LinkedHashMap<>();
        Matcher matcher = ALTER_TABLE.matcher(script);
        while (matcher.find()) {
            List<String> lines = matcher.group(2).lines()
                .map(String::strip)
                .filter(line -> line.startsWith("ADD COLUMN "))
                .map(line -> line.replaceFirst("^ADD COLUMN ", ""))
                .toList();
            tables.computeIfAbsent(matcher.group(1), table -> new ArrayList<>()).addAll(parseColumns(lines));
        }
        return tables;
    }

    private static List<String> parseColumns(List<String> lines) {
        List<String> columns = new ArrayList<>();
        for (String line : lines) {
            String text = line.strip().replaceAll(",$", "");
            Matcher column = COLUMN.matcher(text);
            if (!column.matches()) {
                continue;
            }
            String rest = column.group(3);
            boolean primaryKey = rest.contains("PRIMARY KEY");

            StringBuilder canonical = new StringBuilder(column.group(1))
                .append(' ').append(column.group(2).replace("public.", "").toUpperCase());
            if (rest.contains("NOT NULL") || primaryKey) {
                canonical.append(" NOT NULL");
            }
            appendMatch(canonical, PRIMARY_KEY.matcher(rest), " PK=%s");
            appendMatch(canonical, FOREIGN_KEY.matcher(rest), " FK=%s->%s(%s)");
            appendMatch(canonical, GENERATED.matcher(rest), " GENERATED=%s");
            if (rest.contains("DEFAULT CURRENT_TIMESTAMP")) {
                canonical.append(" DEFAULT");
            }
            if (rest.contains("GENERATED BY DEFAULT AS IDENTITY")) {
                canonical.append(" IDENTITY");
            }
            columns.add(canonical.toString());
        }
        return columns;
    }

    private static void appendMatch(StringBuilder canonical, Matcher matcher, String format) {
        if (matcher.find()) {
            Object[] groups = new Object[matcher.groupCount()];
            for (int i = 0; i < groups.length; i++) {
                groups[i] = matcher.group(i + 1);
            }
            canonical.append(format.formatted(groups));
        }
    }

    private static Set<String> hearingColumnsAfterMigrations() throws IOException {
        Set<String> columns = createdTables(readScript("db/migration/V013__add_hearing_table.sql")).get("hearing")
            .stream().map(column -> column.split(" ")[0]).collect(Collectors.toCollection(LinkedHashSet::new));
        for (String script : List.of("V014__add_hearing_cancelled_flag.sql", "V015__add_hearing_duration_days.sql")) {
            Matcher added = ADD_COLUMN.matcher(readScript("db/migration/" + script));
            while (added.find()) {
                columns.add(added.group(1));
            }
        }
        Matcher dropped = DROP_COLUMN.matcher(readScript("db/migration/V025__remove_notice_parties_from_hearing.sql"));
        while (dropped.find()) {
            columns.remove(dropped.group(1));
        }
        alteredTables(readScript(KEY_COLUMNS_SCRIPT)).get("hearing")
            .forEach(column -> columns.add(column.split(" ")[0]));
        return columns;
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
