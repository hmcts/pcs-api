package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@ExtendWith(MockitoExtension.class)
class ManageHearingPageTest extends BasePageTest {

    @Mock
    private HearingService hearingService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ManageHearingPage(hearingService));
    }

    @Test
    void shouldRetainSelectedHearingIdAsHiddenInternalContext() {
        List<Field> fields = event.getFields().getFields().stream()
            .map(Field.FieldBuilder::build)
            .toList();

        assertThat(fields)
            .filteredOn(field -> "selectedHearingId".equals(field.getId()))
            .singleElement()
            .satisfies(field -> {
                assertThat(field.getShowCondition()).isEqualTo(NEVER_SHOW);
                assertThat(field.getContext()).isEqualTo(DisplayContext.ReadOnly);
                assertThat(field.isRetainHiddenValue()).isTrue();
            });
    }

    @Test
    void shouldShowSeparatorBeforeManageHearingQuestion() {
        List<Field> fields = event.getFields().getFields().stream()
            .map(Field.FieldBuilder::build)
            .toList();

        assertThat(fields)
            .extracting(Field::getId)
            .containsSubsequence("manageHearingSeparator", "manageHearingOption");
    }

    @Test
    void shouldClearHearingFormWhenAddHearingIsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .manageHearingOption(ManageHearingOption.ADD)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        verify(hearingService).clearHearingForm(caseData);
        verify(hearingService, never()).prepopulateEditableHearing(TEST_CASE_REFERENCE, caseData);
        assertThat(response.getData()).isSameAs(caseData);
    }

    @Test
    void shouldPrepopulateHearingFormWhenEditHearingIsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .manageHearingOption(ManageHearingOption.EDIT)
            .selectedHearingId("1")
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        verify(hearingService).prepopulateEditableHearing(TEST_CASE_REFERENCE, caseData);
        verify(hearingService, never()).clearHearingForm(caseData);
        assertThat(response.getData()).isSameAs(caseData);
    }
}
