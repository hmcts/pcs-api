package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppWaTaskServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private TaskDescriptionService taskDescriptionService;
    @Mock
    private CamundaService camundaService;

    private GenAppWaTaskService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppWaTaskService(taskDescriptionService, camundaService);
    }

    @Test
    void shouldCreateTaskForAdjournGenApp() {
        // Given
        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(GenAppType.ADJOURN)
            .build();

        String expectedDescription = "some task description";
        when(taskDescriptionService.createReviewAdjournGenAppDescription(CASE_REFERENCE, genAppEntity))
            .thenReturn(expectedDescription);

        // When
        underTest.createReviewGenAppTask(CASE_REFERENCE, genAppEntity);

        // Then
        verify(camundaService).createTask(CASE_REFERENCE, TaskType.REVIEW_ADJOURN_GEN_APP, expectedDescription);
    }

}
