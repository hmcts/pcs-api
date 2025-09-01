package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeClaimTest extends BasePageTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock(strictness = LENIENT)
    private UnsubmittedCaseDataService unsubmittedCaseDataService;
    @Mock
    private ModelMapper modelMapper;

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new ResumeClaim(unsubmittedCaseDataService, modelMapper));
    }

    @ParameterizedTest
    @MethodSource("unsubmittedDataScenarios")
    void shouldMergeInUnsubmittedData(PCSCase unsubmittedCaseData, YesOrNo keepAnswers, boolean shouldMerge) {
        // Given
        PCSCase caseData = mock(PCSCase.class);

        when(unsubmittedCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE))
            .thenReturn(Optional.ofNullable(unsubmittedCaseData));

        when(caseData.getResumeClaimKeepAnswers()).thenReturn(keepAnswers);

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .id(CASE_REFERENCE)
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "resumeClaim");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        if (shouldMerge) {
            verify(modelMapper).map(unsubmittedCaseData, caseData);
        } else {
            verify(modelMapper, never()).map(any(), eq(caseData));
        }
    }

    private static Stream<Arguments> unsubmittedDataScenarios() {
        PCSCase unsubmittedCaseData = mock(PCSCase.class);

        return Stream.of(
            // Existing unsubmitted data, Keep answers selection, Should merge data
            arguments(null, YesOrNo.YES, false),
            arguments(null, YesOrNo.NO, false),
            arguments(unsubmittedCaseData, YesOrNo.YES, true),
            arguments(unsubmittedCaseData, YesOrNo.NO, false)
        );
    }

}
