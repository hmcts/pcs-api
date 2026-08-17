package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("genAppTypeToTaskTypeScenarios")
    void shouldCreateTaskForGenApp(GenAppType genAppType, TaskType expectedTaskType) {
        // Given
        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(genAppType)
            .build();

        String expectedDescription = "some task description for " + genAppType;
        when(taskDescriptionService.createReviewGenAppDescription(CASE_REFERENCE, genAppEntity))
            .thenReturn(expectedDescription);

        // When
        underTest.createReviewGenAppTask(CASE_REFERENCE, genAppEntity);

        // Then
        verify(camundaService).createTask(CASE_REFERENCE, expectedTaskType, expectedDescription);
    }

    private static Stream<Arguments> genAppTypeToTaskTypeScenarios() {
        return Stream.of(
            Arguments.arguments(GenAppType.ADJOURN, TaskType.REVIEW_ADJOURN_GEN_APP),
            Arguments.arguments(GenAppType.SET_ASIDE, TaskType.REVIEW_SET_ASIDE_GEN_APP),
            Arguments.arguments(GenAppType.SOMETHING_ELSE, TaskType.REVIEW_GEN_APP)
        );
    }


}
