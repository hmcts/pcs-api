package uk.gov.hmcts.reform.pcs.reference.commondata;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDataSourceTest {

    @Test
    void shouldMapEveryHearingReferenceTableToItsCategory() {
        // given
        Map<String, String> expected = Map.ofEntries(
            Map.entry("judge_type", "JudgeType"),
            Map.entry("hearing_channel", "HearingChannel"),
            Map.entry("hearing_priority", "HearingPriority"),
            Map.entry("hearing_case_subtype", "caseSubType"),
            Map.entry("facilities", "Facilities"),
            Map.entry("auto_list_change_reasons", "AutoListChangeReasons"),
            Map.entry("additional_roles", "AdditionalRoles"),
            Map.entry("actual_part_heard_reason", "ActualPartHeardReasonCodes"),
            Map.entry("change_reasons", "ChangeReasons"),
            Map.entry("listing_status", "ListingStatus"),
            Map.entry("actual_cancellation_reason", "ActualCancellationReasonCodes"),
            Map.entry("case_management_cancellation_reason", "CaseManagementCancellationReasons"),
            Map.entry("hearing_type", "HearingType"),
            Map.entry("sign_language", "SignLanguage"),
            Map.entry("unavailable_type", "UnavailableType"),
            Map.entry("hearing_case_type", "caseType"),
            Map.entry("interpreter_language", "InterpreterLanguage"),
            Map.entry("hearing_subchannel", "HearingSubChannel"),
            Map.entry("custody_status", "CustodyStatus"),
            Map.entry("party_relationship_type", "PartyRelationshipType")
        );

        // when
        Map<String, String> actual = Arrays.stream(ReferenceDataSource.values())
            .collect(Collectors.toMap(ReferenceDataSource::getTableName, ReferenceDataSource::getCategoryId));

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldOnlyReferenceTablesCreatedByFlywayWithKeyAndActiveColumns() throws IOException {
        // given
        String migrations = readMigrations();

        // when / then
        for (ReferenceDataSource source : ReferenceDataSource.values()) {
            Matcher definition = Pattern
                .compile("CREATE TABLE public\\." + Pattern.quote(source.getTableName()) + "\\s*\\((.*?)\\);",
                         Pattern.DOTALL)
                .matcher(migrations);

            assertThat(definition.find())
                .as("Flyway creates table %s", source.getTableName())
                .isTrue();
            assertThat(definition.group(1))
                .as("table %s columns", source.getTableName())
                .containsPattern("(?m)^\\s*key\\s")
                .containsPattern("(?m)^\\s*active\\s");
        }
    }

    private String readMigrations() throws IOException {
        StringBuilder migrations = new StringBuilder();
        for (Resource migration : new PathMatchingResourcePatternResolver()
            .getResources("classpath:db/migration/*.sql")) {
            migrations.append(migration.getContentAsString(StandardCharsets.UTF_8)).append('\n');
        }
        return migrations.toString();
    }
}
