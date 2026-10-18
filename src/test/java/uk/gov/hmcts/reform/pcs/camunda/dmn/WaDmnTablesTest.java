package uk.gov.hmcts.reform.pcs.camunda.dmn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariants that hold for every PCS work allocation decision table, whichever task types it covers.
 */
class WaDmnTablesTest {

    private static final Path DMN_DIRECTORY = Path.of("src/main/resources/dmn");

    /**
     * Every permission token wa-task-management-api recognises. An unrecognised one fails the whole task
     * configuration at runtime with {@code IllegalArgumentException: Invalid Permission Type}.
     */
    private static final Set<String> PERMISSIONS = Set.of(
        "Read", "Refer", "Own", "Manage", "Execute", "Cancel", "CancelOwn", "Complete", "CompleteOwn",
        "Claim", "Unclaim", "Assign", "Unassign", "UnassignClaim", "UnassignAssign", "UnclaimAssign");

    /**
     * The attributes wa-task-management-api requires on a configured task and that the PCS table is expected to
     * set for every task type, rather than leaning on a platform default.
     */
    private static final List<String> REQUIRED_CONFIGURATION_ATTRIBUTES = List.of(
        "workType", "roleCategory", "majorPriority", "minorPriority", "dueDateIntervalDays");

    /**
     * The role categories XUI can show and reassign by. wa-task-management-api also has ENFORCEMENT, but neither
     * XUI nor AM do, so a task in it can't be reassigned through XUI (OPEN-QUESTIONS 62).
     */
    private static final Set<String> XUI_ROLE_CATEGORIES = Set.of("JUDICIAL", "LEGAL_OPERATIONS", "ADMIN", "CTSC");

    @ParameterizedTest
    @EnumSource(WaDmn.class)
    void shouldBeWellFormedWithTheExpectedDecisionIdAndHitPolicy(WaDmn dmn) {
        DmnDecisionTable table = dmn.table();

        assertThat(table.decisionId()).isEqualTo(dmn.decisionId);
        assertThat(table.hitPolicy()).isEqualTo(dmn.hitPolicy);
        assertThat(table.rules()).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(WaDmn.class)
    void shouldGiveEveryRuleOneCellPerColumn(WaDmn dmn) {
        DmnDecisionTable table = dmn.table();

        assertThat(table.rules())
            .allSatisfy(rule -> {
                assertThat(rule.inputEntries())
                    .describedAs("input cells of %s in %s", rule.id(), table.fileName())
                    .hasSameSizeAs(table.inputNames());
                assertThat(rule.outputEntries())
                    .describedAs("output cells of %s in %s", rule.id(), table.fileName())
                    .hasSameSizeAs(table.outputNames());
            });
    }

    @ParameterizedTest
    @EnumSource(WaDmn.class)
    void shouldNotRepeatRuleOrOutputNames(WaDmn dmn) {
        DmnDecisionTable table = dmn.table();

        assertThat(table.rules().stream().map(DmnDecisionTable.Rule::id)).doesNotHaveDuplicates();
        assertThat(table.outputNames()).doesNotHaveDuplicates();
    }

    /**
     * Camunda evaluates every rule's cell for a column it reads, so a single cell that is not a FEEL unary test
     * fails the whole decision for every input, not just for the rule holding it. The initiation table came with
     * free text in place of a condition in 23 of its 24 rules, and a real engine does throw on it; nothing
     * depends on that today because nothing publishes PCS case events, but no new rule may add to it.
     */
    @ParameterizedTest
    @EnumSource(WaDmn.class)
    void shouldGiveEveryEnforcementRuleAConditionFeelCanParse(WaDmn dmn) {
        assertThat(dmn.table().cellsThatAreNotLiterals().keySet())
            .noneSatisfy(ruleId -> assertThat(ruleId).startsWith("DecisionRule_enf"));
    }

    @Test
    void shouldCoverEveryDmnFileInTheUploadDirectory() throws IOException {
        try (Stream<Path> files = Files.list(DMN_DIRECTORY)) {
            List<String> onDisk = files
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".dmn"))
                .sorted(naturalOrder())
                .toList();

            assertThat(onDisk).containsExactlyElementsOf(
                Arrays.stream(WaDmn.values()).map(dmn -> dmn.fileName).sorted(naturalOrder()).toList());
        }
    }

    @Test
    void shouldGiveEveryTaskTypeAClaimableRoleAndTheRequiredConfiguration() {
        DmnDecisionTable permissions = WaDmn.PERMISSIONS.table();
        DmnDecisionTable configuration = WaDmn.CONFIGURATION.table();

        assertThat(taskTypeCatalogue().keySet()).allSatisfy(taskType -> {
            Map<String, String> inputs = Map.of("taskType", taskType);

            assertThat(permissions.collect(inputs, "value"))
                .describedAs("a role that can claim %s", taskType)
                .anySatisfy(value -> assertThat(value.split(",")).contains("Own", "Claim"));

            assertThat(configurationAttributes(configuration, taskType))
                .describedAs("configuration attributes of %s", taskType)
                .containsKeys(REQUIRED_CONFIGURATION_ATTRIBUTES.toArray(String[]::new));
        });
    }

    /**
     * wa-task-management-api collects the configuration rows into a map keyed by name without a merge function
     * (CaseConfigurationProviderService.extractDmnResults), so two rows with the same name for one task type throw
     * "Duplicate key" and leave the task unconfigured. RULE ORDER does not make the last one win.
     */
    @Test
    void shouldGiveEachTaskTypeOneConfigurationRowPerAttribute() {
        DmnDecisionTable configuration = WaDmn.CONFIGURATION.table();

        assertThat(taskTypeCatalogue().keySet()).allSatisfy(taskType ->
            assertThat(configuration.collect(Map.of("taskType", taskType), "name"))
                .describedAs("configuration attribute names of %s", taskType)
                .doesNotHaveDuplicates());
    }

    @Test
    void shouldUseOnlyRoleCategoriesXuiKnows() {
        DmnDecisionTable configuration = WaDmn.CONFIGURATION.table();

        assertThat(taskTypeCatalogue().keySet()).allSatisfy(taskType ->
            assertThat(configurationAttributes(configuration, taskType).get("roleCategory"))
                .describedAs("role category of %s", taskType)
                .isIn(XUI_ROLE_CATEGORIES));
        DmnDecisionTable permissions = WaDmn.PERMISSIONS.table();
        int roleCategory = permissions.outputNames().indexOf("roleCategory");
        assertThat(permissions.rules())
            .allSatisfy(rule -> assertThat(rule.outputEntries().get(roleCategory).replace("\"", ""))
                .describedAs("role category of %s", rule.id())
                .isIn(XUI_ROLE_CATEGORIES));
    }

    @Test
    void shouldUseOnlyRecognisedPermissionTokens() {
        assertThat(WaDmn.PERMISSIONS.table().rules())
            .allSatisfy(rule -> {
                String value = rule.outputEntries().get(2).replace("\"", "");
                assertThat(value.isEmpty() ? new String[0] : value.split(","))
                    .describedAs("permissions of %s", rule.id())
                    .allMatch(PERMISSIONS::contains);
            });
    }

    @Test
    void shouldListEveryTaskTypeTheApplicationCanCreateInTheCatalogue() {
        assertThat(taskTypeCatalogue()).containsKeys(
            Arrays.stream(TaskType.values()).map(TaskType::getId).distinct().toArray(String[]::new));
    }

    /**
     * The task type id to name catalogue XUI reads for its task type filters.
     */
    static Map<String, String> taskTypeCatalogue() {
        Map<String, String> catalogue = new LinkedHashMap<>();
        for (Map<String, String> row : WaDmn.TYPES.table().evaluate(Map.of())) {
            String taskTypeId = row.get("taskTypeId");
            if (!taskTypeId.isEmpty()) {
                catalogue.put(taskTypeId, row.get("taskTypeName"));
            }
        }
        return catalogue;
    }

    /**
     * The configuration attributes of a task type: every matching rule contributes. A name that two matching rules
     * set is a configuration error ({@link #shouldGiveEachTaskTypeOneConfigurationRowPerAttribute}), so which of
     * them this returns does not matter.
     */
    static Map<String, String> configurationAttributes(DmnDecisionTable configuration, String taskType) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (Map<String, String> row : configuration.evaluate(Map.of("taskType", taskType))) {
            attributes.put(row.get("name"), row.get("value"));
        }
        return attributes;
    }

    enum WaDmn {
        INITIATION("wa-task-initiation-possession_service.dmn", "wa-task-initiation-pcs-pcs", "COLLECT"),
        CONFIGURATION("wa-task-configuration-possession_service.dmn", "wa-task-configuration-pcs-pcs",
                      "RULE ORDER"),
        PERMISSIONS("wa-task-permissions-possession_service.dmn", "wa-task-permissions-pcs-pcs", "RULE ORDER"),
        CANCELLATION("wa-task-cancellation-possession_service.dmn", "wa-task-cancellation-pcs-pcs", "COLLECT"),
        COMPLETION("wa-task-completion-possession_service.dmn", "wa-task-completion-pcs-pcs", "COLLECT"),
        TYPES("wa-task-types-possession_service.dmn", "wa-task-types-pcs-pcs", "COLLECT");

        private final String fileName;
        private final String decisionId;
        private final String hitPolicy;

        WaDmn(String fileName, String decisionId, String hitPolicy) {
            this.fileName = fileName;
            this.decisionId = decisionId;
            this.hitPolicy = hitPolicy;
        }

        DmnDecisionTable table() {
            return DmnDecisionTable.load(fileName);
        }
    }
}
