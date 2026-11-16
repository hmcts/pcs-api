package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;

import java.util.List;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingSummaryRenderer;
import uk.gov.hmcts.reform.pcs.exception.HearingNotFoundException;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import static org.mockito.Mockito.doAnswer;

import static org.assertj.core.api.Assertions.assertThat;
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
        PCSCase caseData = PCSCase.builder().manageHearingOption(ManageHearingOption.ADD).build();

        assertThat(callMidEventHandler(caseData).getData()).isSameAs(caseData);

        verify(hearingService).clearHearingForm(caseData);
    }

    @Test
    void shouldLoadTheSelectedHearingForEditing() {
        PCSCase caseData = selected(ManageHearingOption.EDIT);
        HearingEntity hearing = HearingEntity.builder().id(2).build();
        when(hearingService.requireEditableHearing(TEST_CASE_REFERENCE, "2")).thenReturn(hearing);
        doAnswer(invocation -> {
            caseData.setHearing(Hearing.builder().hearingId(2).build());
            return null;
        }).when(hearingService).initialiseEditableHearing(TEST_CASE_REFERENCE, caseData, "1");
        when(hearingSummaryRenderer.renderMarkdown(hearing, null)).thenReturn("Selected hearing summary");

        PCSCase result = callMidEventHandler(caseData).getData();

        assertThat(result.getSelectedHearingId()).isEqualTo("2");
        assertThat(result.getHearing().getHearingId()).isEqualTo(2);
        assertThat(result.getHearing().getHearingSummaryMarkdown()).isEqualTo("Selected hearing summary");
    }

    @Test
    void shouldLoadTheSelectedHearingForCancellation() {
        PCSCase caseData = selected(ManageHearingOption.CANCEL);
        HearingEntity hearing = HearingEntity.builder().id(2).build();
        when(hearingService.requireEditableHearing(TEST_CASE_REFERENCE, "2")).thenReturn(hearing);
        doAnswer(invocation -> {
            caseData.setHearing(Hearing.builder().hearingId(2).build());
            return null;
        }).when(hearingService).initialiseEditableHearing(TEST_CASE_REFERENCE, caseData, "1");
        when(hearingSummaryRenderer.renderMarkdown(hearing, null)).thenReturn("Selected hearing summary");

        PCSCase result = callMidEventHandler(caseData).getData();

        assertThat(result.getHearing().getHearingId()).isEqualTo(2);
        assertThat(result.getHearing().getHearingSummaryMarkdown()).isEqualTo("Selected hearing summary");
    }

    @Test
    void shouldAskForAnAvailableHearingWhenSelectionIsInvalid() {
        PCSCase caseData = selected(ManageHearingOption.EDIT);
        when(hearingService.requireEditableHearing(TEST_CASE_REFERENCE, "2"))
            .thenThrow(new HearingNotFoundException("Select an available hearing on this case"));

        assertThat(callMidEventHandler(caseData).getErrors())
            .containsExactly("Select an available hearing on this case");
    }

    private PCSCase selected(ManageHearingOption option) {
        return PCSCase.builder().manageHearingOption(option).selectedHearingId("1")
            .hearingSelection(DynamicStringList.builder()
                .value(DynamicStringListElement.builder().code("2").build()).build())
            .build();
    }
}
