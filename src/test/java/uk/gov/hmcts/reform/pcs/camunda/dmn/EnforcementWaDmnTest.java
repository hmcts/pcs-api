package uk.gov.hmcts.reform.pcs.camunda.dmn;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.camunda.dmn.WaDmnTablesTest.WaDmn;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.ALLOCATE_CASE_TO_BAILIFF;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.MARK_NOTICE_OF_EVICTION_SERVED;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.RECORD_WARRANT_RETURN;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.RESCHEDULE_EVICTION;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_CLAIMANT_WARRANT_APPLICATION;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_RISKS_AND_SET_LEVEL;
import static uk.gov.hmcts.reform.pcs.camunda.TaskType.REVIEW_UPDATED_RISK_INFORMATION;

/**
 * The enforcement rows of the PCS work allocation decision tables, against the triggers, completions and
 * cancellations in 18-work-allocation-tasks.feature.
 */
class EnforcementWaDmnTest {

    /**
     * The task types an enforcement raises for the bailiff the warrant is allocated to.
     */
    private static final List<TaskType> ALLOCATED_BAILIFF_TASKS = List.of(
        REVIEW_RISKS_AND_SET_LEVEL, MARK_NOTICE_OF_EVICTION_SERVED, RESCHEDULE_EVICTION,
        REVIEW_UPDATED_RISK_INFORMATION, RECORD_WARRANT_RETURN);

    @Nested
    class Initiation {

        private final DmnDecisionTable initiation = WaDmn.INITIATION.table();

        @Test
        void shouldRaiseTheReviewTaskWhenAnApplicationReachesReview() {
            Map<String, String> row = onlyRow(Map.of(
                "eventId", "enforceTheOrder",
                "enforcementStatus", "AWAITING_REVIEW"));

            assertThat(row.get("taskId")).isEqualTo(REVIEW_CLAIMANT_WARRANT_APPLICATION.getId());
            assertThat(row.get("name")).isEqualTo(REVIEW_CLAIMANT_WARRANT_APPLICATION.getName());
            assertThat(row.get("workingDaysAllowed")).isEqualTo("10");
            assertThat(row.get("workType")).isEqualTo("decision_making_work");
            assertThat(row.get("roleCategory")).isEqualTo("ADMIN");
        }

        @Test
        void shouldRaiseTheAllocationTaskWhenAWarrantIsApproved() {
            assertThat(onlyRow(Map.of(
                "eventId", "decideEnforcement",
                "enforcementStatus", "AWAITING_BAILIFF_ALLOCATION")))
                .containsEntry("taskId", ALLOCATE_CASE_TO_BAILIFF.getId())
                .containsEntry("name", ALLOCATE_CASE_TO_BAILIFF.getName())
                .containsEntry("roleCategory", "ENFORCEMENT");
        }

        @Test
        void shouldRaiseTheAllocationTaskAgainWhenAReturnReopensTheWarrant() {
            assertThat(onlyRow(Map.of(
                "eventId", "recordWarrantReturn",
                "enforcementStatus", "AWAITING_BAILIFF_ALLOCATION")))
                .containsEntry("taskId", ALLOCATE_CASE_TO_BAILIFF.getId());
        }

        @Test
        void shouldRaiseTheRiskTaskWhenABailiffIsAllocated() {
            assertThat(onlyRow(Map.of(
                "eventId", "allocateBailiff",
                "enforcementStatus", "AWAITING_SCHEDULING")))
                .containsEntry("taskId", REVIEW_RISKS_AND_SET_LEVEL.getId())
                .containsEntry("workType", "decision_making_work");
        }

        @ParameterizedTest
        @CsvSource({"scheduleEviction", "rescheduleEviction"})
        void shouldRaiseTheServiceTaskWhenThereIsANoticeOfEvictionToServe(String eventId) {
            assertThat(onlyRow(Map.of(
                "eventId", eventId,
                "canMarkNoticeOfEvictionServed", "Yes")))
                .containsEntry("taskId", MARK_NOTICE_OF_EVICTION_SERVED.getId());
        }

        @ParameterizedTest
        @CsvSource({"scheduleEviction", "rescheduleEviction"})
        void shouldNotRaiseTheServiceTaskWhenServiceWasDispensedWith(String eventId) {
            assertThat(initiation.evaluate(Map.of(
                "eventId", eventId,
                "canMarkNoticeOfEvictionServed", "No"))).isEmpty();
        }

        @Test
        void shouldRaiseTheRescheduleTaskWhenTheClaimantCannotAttend() {
            assertThat(onlyRow(Map.of(
                "eventId", "confirmEviction",
                "enforcementStatus", "CLAIMANT_UNABLE_TO_ATTEND")))
                .containsEntry("taskId", RESCHEDULE_EVICTION.getId());
        }

        @ParameterizedTest
        @CsvSource({"decideEnforcement,REJECTED", "decideEnforcement,TRANSFERRED_TO_HIGH_COURT",
            "recordWarrantReturn,CLOSED", "allocateBailiff,AWAITING_BAILIFF_ALLOCATION"})
        void shouldRaiseNothingForAnEnforcementStatusNoTaskBelongsTo(String eventId, String enforcementStatus) {
            assertThat(initiation.evaluate(Map.of("eventId", eventId, "enforcementStatus", enforcementStatus)))
                .isEmpty();
        }

        /**
         * There is no case data field saying the claimant changed a risk assessment answer (HDPI-7552), so the
         * updated risk task has no initiation rule at all rather than one that fires on every confirmation.
         */
        @Test
        void shouldHaveNoRuleForTheUpdatedRiskTask() {
            assertThat(initiation.rules())
                .noneSatisfy(rule -> assertThat(rule.outputEntries().getFirst())
                    .isEqualTo("\"%s\"".formatted(REVIEW_UPDATED_RISK_INFORMATION.getId())));

            assertThat(initiation.evaluate(Map.of(
                "eventId", "confirmEviction",
                "enforcementStatus", "CLAIMANT_CONFIRMED"))).isEmpty();
        }

        private Map<String, String> onlyRow(Map<String, String> inputs) {
            List<Map<String, String>> rows = initiation.evaluate(inputs);
            assertThat(rows).hasSize(1);
            return rows.getFirst();
        }
    }

    @Nested
    class Completion {

        private final DmnDecisionTable completion = WaDmn.COMPLETION.table();

        @ParameterizedTest
        @CsvSource({
            "decideEnforcement,ReviewClaimantWarrantApplication",
            "allocateBailiff,AllocateCaseToBailiff",
            "markNoticeOfEvictionServed,MarkNoticeOfEvictionServed",
            "rescheduleEviction,RescheduleEviction",
            "recordWarrantReturn,RecordWarrantReturn"
        })
        void shouldCompleteTheTaskTheEventFinishes(String eventId, String taskType) {
            assertThat(completion.collect(Map.of("eventId", eventId), "taskType")).containsExactly(taskType);
        }

        @Test
        void shouldCompleteBothRiskTasksOnSetRiskLevel() {
            assertThat(completion.collect(Map.of("eventId", "setRiskLevel"), "taskType"))
                .containsExactly(REVIEW_RISKS_AND_SET_LEVEL.getId(), REVIEW_UPDATED_RISK_INFORMATION.getId());
        }

        @ParameterizedTest
        @EnumSource(value = TaskType.class, names = {
            "REVIEW_CLAIMANT_WARRANT_APPLICATION", "ALLOCATE_CASE_TO_BAILIFF", "REVIEW_RISKS_AND_SET_LEVEL",
            "MARK_NOTICE_OF_EVICTION_SERVED", "RESCHEDULE_EVICTION", "REVIEW_UPDATED_RISK_INFORMATION",
            "RECORD_WARRANT_RETURN"})
        void shouldCompleteEveryEnforcementTaskAutomatically(TaskType taskType) {
            assertThat(completion.rules())
                .filteredOn(rule -> rule.outputEntries().getFirst().equals("\"%s\"".formatted(taskType.getId())))
                .isNotEmpty()
                .allSatisfy(rule -> assertThat(rule.outputEntries().get(1)).isEqualTo("\"Auto\""));
        }
    }

    @Nested
    class Cancellation {

        private final DmnDecisionTable cancellation = WaDmn.CANCELLATION.table();

        @Test
        void shouldCancelEveryOpenBailiffTaskWhenAReturnClosesTheEnforcement() {
            assertThat(cancelledBy("CLOSED"))
                .containsExactlyInAnyOrder(
                    ALLOCATE_CASE_TO_BAILIFF.getId(),
                    REVIEW_RISKS_AND_SET_LEVEL.getId(),
                    MARK_NOTICE_OF_EVICTION_SERVED.getId(),
                    RESCHEDULE_EVICTION.getId(),
                    REVIEW_UPDATED_RISK_INFORMATION.getId(),
                    RECORD_WARRANT_RETURN.getId());
        }

        /**
         * A return that sends the warrant back for scheduling, or pauses it for breathing space, voids the
         * eviction date without ending the allocation, so the allocated bailiff keeps their risk tasks.
         */
        @ParameterizedTest
        @CsvSource({"AWAITING_SCHEDULING", "BREATHING_SPACE"})
        void shouldCancelOnlyTheEvictionDateTasksForAReturnThatKeepsTheBailiff(String enforcementStatus) {
            assertThat(cancelledBy(enforcementStatus))
                .containsExactlyInAnyOrder(
                    MARK_NOTICE_OF_EVICTION_SERVED.getId(),
                    RESCHEDULE_EVICTION.getId(),
                    RECORD_WARRANT_RETURN.getId());
        }

        /**
         * A reopened warrant has no bailiff, so the allocated bailiff's tasks go as well. The allocation task
         * itself is not cancelled: the reopen is what raises it.
         */
        @Test
        void shouldCancelTheAllocatedBailiffsTasksWhenAReturnReopensTheWarrant() {
            assertThat(cancelledBy("AWAITING_BAILIFF_ALLOCATION"))
                .doesNotContain(ALLOCATE_CASE_TO_BAILIFF.getId())
                .containsExactlyInAnyOrder(
                    REVIEW_RISKS_AND_SET_LEVEL.getId(),
                    MARK_NOTICE_OF_EVICTION_SERVED.getId(),
                    RESCHEDULE_EVICTION.getId(),
                    REVIEW_UPDATED_RISK_INFORMATION.getId(),
                    RECORD_WARRANT_RETURN.getId());
        }

        /**
         * A new eviction date needs a new return task due on it, so the one due on the old date is cancelled
         * rather than left behind.
         */
        @Test
        void shouldCancelTheReturnTaskWhenTheEvictionIsRescheduled() {
            Map<String, String> reschedule = Map.of("event", "rescheduleEviction");

            assertThat(cancellation.collect(reschedule, "action")).containsOnly("Cancel");
            assertThat(cancellation.collect(reschedule, "processCategories"))
                .containsExactly(RECORD_WARRANT_RETURN.getId());
        }

        @Test
        void shouldCancelNothingForAReturnThatLeavesTheStatusUnchanged() {
            assertThat(cancellation.collect(Map.of("event", "recordWarrantReturn"), "action")).isEmpty();
        }

        private List<String> cancelledBy(String enforcementStatus) {
            Map<String, String> inputs = Map.of(
                "event", "recordWarrantReturn",
                "enforcementStatus", enforcementStatus);

            assertThat(cancellation.collect(inputs, "action")).containsOnly("Cancel");
            return cancellation.collect(inputs, "processCategories");
        }
    }

    @Nested
    class Configuration {

        private final DmnDecisionTable configuration = WaDmn.CONFIGURATION.table();

        @Test
        void shouldGiveTheReviewTaskTenWorkingDaysAndPriorityBandTwo() {
            assertThat(attributesOf(REVIEW_CLAIMANT_WARRANT_APPLICATION))
                .containsEntry("workType", "decision_making_work")
                .containsEntry("roleCategory", "ADMIN")
                .containsEntry("majorPriority", "2000")
                .containsEntry("minorPriority", "500")
                .containsEntry("dueDateIntervalDays", "10");
        }

        @ParameterizedTest
        @EnumSource(value = TaskType.class, names = {
            "ALLOCATE_CASE_TO_BAILIFF", "REVIEW_RISKS_AND_SET_LEVEL", "MARK_NOTICE_OF_EVICTION_SERVED",
            "RESCHEDULE_EVICTION", "REVIEW_UPDATED_RISK_INFORMATION", "RECORD_WARRANT_RETURN"})
        void shouldPutBailiffTasksInTheEnforcementRoleCategory(TaskType taskType) {
            assertThat(attributesOf(taskType))
                .containsEntry("roleCategory", "ENFORCEMENT")
                .containsEntry("majorPriority", "5000")
                .containsEntry("dueDateIntervalDays", "5");
        }

        @ParameterizedTest
        @CsvSource({
            "ALLOCATE_CASE_TO_BAILIFF,routine_work",
            "REVIEW_RISKS_AND_SET_LEVEL,decision_making_work",
            "MARK_NOTICE_OF_EVICTION_SERVED,routine_work",
            "RESCHEDULE_EVICTION,routine_work",
            "REVIEW_UPDATED_RISK_INFORMATION,decision_making_work",
            "RECORD_WARRANT_RETURN,routine_work"
        })
        void shouldSetTheWorkTypeThatPutsTheTaskOnTheRightMyWorkList(TaskType taskType, String workType) {
            assertThat(attributesOf(taskType)).containsEntry("workType", workType);
        }

        private Map<String, String> attributesOf(TaskType taskType) {
            return WaDmnTablesTest.configurationAttributes(configuration, taskType.getId());
        }
    }

    @Nested
    class Permissions {

        private final DmnDecisionTable permissions = WaDmn.PERMISSIONS.table();

        @Test
        void shouldGiveTheReviewTaskToTheHearingCentreRolesAlreadyInTheTable() {
            assertThat(permissions.collect(inputsFor(REVIEW_CLAIMANT_WARRANT_APPLICATION), "name"))
                .containsExactly("hearing-centre-admin", "hearing-centre-team-leader");
            assertThat(permissions.collect(inputsFor(REVIEW_CLAIMANT_WARRANT_APPLICATION), "roleCategory"))
                .containsOnly("ADMIN");
        }

        @Test
        void shouldGiveTheAllocationTaskToEveryRoleThatAllocates() {
            assertThat(permissions.collect(inputsFor(ALLOCATE_CASE_TO_BAILIFF), "name"))
                .containsExactly("bailiff", "bailiff-admin", "bailiff-manager");
        }

        @Test
        void shouldGiveTheRemainingTasksToTheAllocatedBailiffAndTheirManager() {
            assertThat(ALLOCATED_BAILIFF_TASKS).allSatisfy(taskType ->
                assertThat(permissions.collect(inputsFor(taskType), "name"))
                    .containsExactly("allocated-bailiff", "bailiff-manager"));
        }

        @Test
        void shouldAutoAssignTheAllocatedBailiffAndNobodyElse() {
            assertThat(ALLOCATED_BAILIFF_TASKS).allSatisfy(taskType ->
                assertThat(permissions.evaluate(inputsFor(taskType)))
                    .filteredOn(row -> "true".equals(row.get("autoAssignable")))
                    .singleElement()
                    .satisfies(row -> {
                        assertThat(row.get("name")).isEqualTo("allocated-bailiff");
                        assertThat(row.get("assignmentPriority")).isEqualTo("1");
                        assertThat(row.get("value").split(",")).contains("Own", "Claim");
                    }));
        }

        @Test
        void shouldLetTheManagerReassignBailiffTasks() {
            assertThat(ALLOCATED_BAILIFF_TASKS).allSatisfy(taskType ->
                assertThat(permissions.evaluate(inputsFor(taskType)))
                    .filteredOn(row -> "bailiff-manager".equals(row.get("name")))
                    .singleElement()
                    .satisfies(row -> assertThat(row.get("value").split(","))
                        .contains("Assign", "Unassign", "Cancel")));
        }

        private Map<String, String> inputsFor(TaskType taskType) {
            return Map.of("taskType", taskType.getId());
        }
    }

    @Nested
    class TaskTypes {

        @ParameterizedTest
        @EnumSource(value = TaskType.class, names = {
            "REVIEW_CLAIMANT_WARRANT_APPLICATION", "ALLOCATE_CASE_TO_BAILIFF", "REVIEW_RISKS_AND_SET_LEVEL",
            "MARK_NOTICE_OF_EVICTION_SERVED", "RESCHEDULE_EVICTION", "REVIEW_UPDATED_RISK_INFORMATION",
            "RECORD_WARRANT_RETURN"})
        void shouldCatalogueEachEnforcementTaskTypeUnderTheNameTheApplicationSends(TaskType taskType) {
            assertThat(WaDmnTablesTest.taskTypeCatalogue())
                .containsEntry(taskType.getId(), taskType.getName());
        }
    }
}
