package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.nonprod.CaseSupportHelper;
import uk.gov.hmcts.reform.pcs.ccd.service.nonprod.NonProdSupportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NonProdSupportTest extends BaseEventTest {

    private NonProdSupport underTest;

    @Mock
    private NonProdSupportService nonProdSupportService;
    @Mock
    private CaseSupportHelper caseSupportHelper;
    @Mock
    private DynamicList dynamicList;

    @BeforeEach
    void setUp() {
        underTest = new NonProdSupport(nonProdSupportService, caseSupportHelper);
        setEventUnderTest(underTest);
    }

    @Test
    void shouldSetFeeAmountWhen123Point45StartMethodIsInvoked() {
        // Given
        when(caseSupportHelper.getNonProdFilesList()).thenReturn(dynamicList);
        PCSCase caseData = PCSCase.builder().build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getFeeAmount()).isEqualTo("123.45");
        assertThat(result.getNonProdSupportFileList()).isEqualTo(dynamicList);
        verify(caseSupportHelper).getNonProdFilesList();
    }

    @Test
    void shouldPopulateNonProdSupportFileListWithDynamicListFromCaseSupportHelperDuringStart() {
        // Given
        when(caseSupportHelper.getNonProdFilesList()).thenReturn(dynamicList);
        PCSCase caseData = PCSCase.builder().build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getNonProdSupportFileList()).isSameAs(dynamicList);
        verify(caseSupportHelper).getNonProdFilesList();
    }

    @Test
    void shouldReturnSubmitResponseWithCaseIssuedStateAfterSuccessfulSubmission() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

        // When
        SubmitResponse<State> result = callSubmitHandler(caseData);

        // Then
        assertThat(result.getState()).isEqualTo(State.CASE_ISSUED);
        verify(nonProdSupportService).caseGenerator(anyLong(), any());
    }

}
