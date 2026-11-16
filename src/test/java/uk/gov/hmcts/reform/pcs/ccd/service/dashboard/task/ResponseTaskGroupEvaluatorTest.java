package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.RESPOND_TO_CLAIM;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.VIEW_RESPONSE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.YOUR_SUPPORT;

class ResponseTaskGroupEvaluatorTest {

    private final ResponseTaskGroupEvaluator underTest = new ResponseTaskGroupEvaluator();

    @Test
    void shouldReturnResponseGroupId() {
        assertThat(underTest.groupId()).isEqualTo(TaskGroupId.RESPONSE);
    }

    @Test
    void shouldOnlyOfferRespondToClaimBeforeAnyResponse() {
        TaskGroup taskGroup = underTest.evaluate(context(false, false));

        assertTaskStatuses(taskGroup, TaskStatus.NOT_STARTED, TaskStatus.NOT_AVAILABLE, TaskStatus.NOT_AVAILABLE);
    }

    @Test
    void shouldMarkRespondToClaimInProgressWhileADraftExists() {
        TaskGroup taskGroup = underTest.evaluate(context(true, false));

        assertTaskStatuses(taskGroup, TaskStatus.IN_PROGRESS, TaskStatus.NOT_AVAILABLE, TaskStatus.NOT_AVAILABLE);
    }

    @Test
    void shouldOpenViewResponseAndYourSupportOnceTheResponseIsSubmitted() {
        TaskGroup taskGroup = underTest.evaluate(context(false, true));

        assertTaskStatuses(taskGroup, TaskStatus.COMPLETED, TaskStatus.AVAILABLE, TaskStatus.AVAILABLE);
    }

    @Test
    void shouldTreatASubmittedResponseAsAuthoritativeWhenADraftAlsoExists() {
        TaskGroup taskGroup = underTest.evaluate(context(true, true));

        assertTaskStatuses(taskGroup, TaskStatus.COMPLETED, TaskStatus.AVAILABLE, TaskStatus.AVAILABLE);
    }

    private static DashboardContext context(boolean hasDraftResponse, boolean hasSubmittedResponse) {
        return new DashboardContext(100L, PcsCaseEntity.builder().build(), null, hasDraftResponse,
                                    hasSubmittedResponse);
    }

    private static void assertTaskStatuses(TaskGroup taskGroup,
                                           TaskStatus respondToClaimStatus,
                                           TaskStatus viewResponseStatus,
                                           TaskStatus yourSupportStatus) {
        assertThat(taskGroup.getTasks())
            .extracting(ListValue::getValue)
            .extracting(Task::getTemplateId, Task::getStatus)
            .containsExactly(
                tuple(RESPOND_TO_CLAIM, respondToClaimStatus),
                tuple(VIEW_RESPONSE, viewResponseStatus),
                tuple(YOUR_SUPPORT, yourSupportStatus)
            );
    }
}
