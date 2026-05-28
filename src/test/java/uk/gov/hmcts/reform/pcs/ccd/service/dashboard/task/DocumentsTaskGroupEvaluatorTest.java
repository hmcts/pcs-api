package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.UPLOAD_DOCUMENTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.VIEW_DOCUMENTS;

class DocumentsTaskGroupEvaluatorTest {

    private final DocumentsTaskGroupEvaluator underTest = new DocumentsTaskGroupEvaluator();

    @Test
    void shouldReturnDocumentsGroupId() {
        assertThat(underTest.groupId()).isEqualTo(TaskGroupId.DOCUMENTS);
    }

    @Test
    void shouldMarkViewDocumentsAsAvailableWhenCaseHasDocuments() {
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .documents(List.of(DocumentEntity.builder().build()))
            .build();

        TaskGroup taskGroup = underTest.evaluate(new DashboardContext(100L, caseEntity, null, false, false));

        assertTaskStatuses(taskGroup, TaskStatus.AVAILABLE);
    }

    @Test
    void shouldMarkViewDocumentsAsNotAvailableWhenCaseHasNoDocuments() {
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .documents(List.of())
            .build();

        TaskGroup taskGroup = underTest.evaluate(new DashboardContext(100L, caseEntity, null, false, false));

        assertTaskStatuses(taskGroup, TaskStatus.NOT_AVAILABLE);
    }

    @Test
    void shouldMarkViewDocumentsAsNotAvailableWhenContextMissing() {
        TaskGroup taskGroup = underTest.evaluate(null);

        assertTaskStatuses(taskGroup, TaskStatus.NOT_AVAILABLE);
    }

    private void assertTaskStatuses(TaskGroup taskGroup, TaskStatus viewDocumentsStatus) {
        assertThat(taskGroup.getTasks())
            .extracting(ListValue::getValue)
            .extracting(Task::getTemplateId, Task::getStatus)
            .containsExactly(
                tuple(UPLOAD_DOCUMENTS, TaskStatus.AVAILABLE),
                tuple(VIEW_DOCUMENTS, viewDocumentsStatus)
            );
    }
}
