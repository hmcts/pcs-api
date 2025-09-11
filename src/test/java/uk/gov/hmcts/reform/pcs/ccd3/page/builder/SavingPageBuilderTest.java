package uk.gov.hmcts.reform.pcs.ccd3.page.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd3.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.service.UnsubmittedCaseDataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingPageBuilderTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String TEST_PAGE_ID = "test-page";

    @Mock
    private UnsubmittedCaseDataService unsubmittedCaseDataService;
    @Mock
    private EventBuilder<PCSCase, UserRole, State> eventBuilder;
    @Mock
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldCollectionBuilder;
    @Captor
    private ArgumentCaptor<MidEvent<PCSCase, State>> midEventCaptor;

    private SavingPageBuilder underTest;

    @BeforeEach
    void setUp() {
        when(eventBuilder.fields()).thenReturn(fieldCollectionBuilder);
        underTest = new SavingPageBuilder(unsubmittedCaseDataService, eventBuilder);
    }

    @Test
    void shouldAddMidEventToSaveUnsubmittedCaseData() {
        // Given
        underTest.page(TEST_PAGE_ID);

        PCSCase caseData = mock(PCSCase.class);

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .id(CASE_REFERENCE)
            .data(caseData)
            .build();

        CaseDetails<PCSCase, State> caseDetailsBefore = new CaseDetails<>();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = getMidEventHandler()
            .handle(caseDetails, caseDetailsBefore);

        // Then
        verify(unsubmittedCaseDataService).saveUnsubmittedCaseData(CASE_REFERENCE, caseData);
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldWrapPageMidEventToSaveUnsubmittedCaseData() {
        // Given
        MidEvent<PCSCase, State> pageMidEvent = mock(MidEvent.class);
        AboutToStartOrSubmitResponse<PCSCase, State> pageMidEventResponse = mock(AboutToStartOrSubmitResponse.class);
        when(pageMidEvent.handle(any(), any())).thenReturn(pageMidEventResponse);

        underTest.page(TEST_PAGE_ID, pageMidEvent);

        PCSCase caseData = mock(PCSCase.class);

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .id(CASE_REFERENCE)
            .data(caseData)
            .build();

        CaseDetails<PCSCase, State> caseDetailsBefore = new CaseDetails<>();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = getMidEventHandler()
            .handle(caseDetails, caseDetailsBefore);

        // Then
        InOrder inOrder = Mockito.inOrder(pageMidEvent, unsubmittedCaseDataService);
        inOrder.verify(pageMidEvent).handle(caseDetails, caseDetailsBefore);
        inOrder.verify(unsubmittedCaseDataService).saveUnsubmittedCaseData(CASE_REFERENCE, caseData);
        assertThat(response).isEqualTo(pageMidEventResponse);
    }

    private MidEvent<PCSCase, State> getMidEventHandler() {
        verify(fieldCollectionBuilder).page(eq(TEST_PAGE_ID), midEventCaptor.capture());
        return midEventCaptor.getValue();
    }

}
