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
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingSummaryRenderer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@ExtendWith(MockitoExtension.class)
class ManageHearingPageTest extends BasePageTest {

    @Mock
    private HearingService hearingService;
    @Mock
    private HearingSummaryRenderer hearingSummaryRenderer;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ManageHearingPage(hearingService, hearingSummaryRenderer));
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
    void shouldRetainDraftHearingFieldsAsHiddenInternalContext() {
        List<Field> fields = event.getFields().getFields().stream()
            .map(Field.FieldBuilder::build)
            .toList();

        assertThat(fields)
            .filteredOn(field -> "mhDraft_Type".equals(field.getId()))
            .singleElement()
            .satisfies(field -> {
                assertThat(field.getShowCondition()).isEqualTo(NEVER_SHOW);
                assertThat(field.getContext()).isEqualTo(DisplayContext.ReadOnly);
                assertThat(field.isRetainHiddenValue()).isTrue();
            });
        assertThat(fields)
            .filteredOn(field -> "mhDraft_NoticeWording".equals(field.getId()))
            .singleElement()
            .satisfies(field -> {
                assertThat(field.getShowCondition()).isEqualTo(NEVER_SHOW);
                assertThat(field.getContext()).isEqualTo(DisplayContext.ReadOnly);
                assertThat(field.isRetainHiddenValue()).isTrue();
            });
        assertThat(fields)
            .filteredOn(field -> "mhDraftPartyList".equals(field.getId()))
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
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        verify(hearingService).initialiseEditableHearing(TEST_CASE_REFERENCE, caseData, null);
        verify(hearingService, never()).clearHearingForm(caseData);
        assertThat(response.getData()).isSameAs(caseData);
    }

    @Test
    void shouldPassPreviousSelectedHearingIdWhenEditHearingAlreadyHasDraftChanges() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .manageHearingOption(ManageHearingOption.EDIT)
            .selectedHearingId("1")
            .hearing(Hearing.builder()
                .notes("Updated notes entered before CYA")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        verify(hearingService).initialiseEditableHearing(TEST_CASE_REFERENCE, caseData, "1");
        verify(hearingService, never()).clearHearingForm(caseData);
        assertThat(response.getData()).isSameAs(caseData);
    }

    @Test
    void shouldRefreshCancellableHearingWhenCancelHearingIsSelected() {
        // Given
        HearingEntity hearingEntity = HearingEntity.builder()
            .id(1)
            .build();
        PCSCase caseData = PCSCase.builder()
            .manageHearingOption(ManageHearingOption.CANCEL)
            .hearingLocation("Central London County Court")
            .hearing(Hearing.builder()
                .notes("edit page state")
                .build())
            .build();

        when(hearingService.findEditableHearing(TEST_CASE_REFERENCE)).thenReturn(Optional.of(hearingEntity));
        when(hearingSummaryRenderer.renderMarkdown(hearingEntity, "Central London County Court"))
            .thenReturn("fresh summary");

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getHearing().getHearingId()).isEqualTo(1);
        assertThat(response.getData().getHearing().getHearingSummaryMarkdown()).isEqualTo("fresh summary");
        assertThat(response.getData().getHearing().getNotes()).isEqualTo("edit page state");
        verify(hearingService, never()).clearHearingForm(caseData);
    }
}
