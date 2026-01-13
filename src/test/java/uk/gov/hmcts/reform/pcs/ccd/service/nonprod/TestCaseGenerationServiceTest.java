package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.nonprod.TestCaseGenerationService.FAILED_TO_GENERATE_TEST_CASE;
import static uk.gov.hmcts.reform.pcs.ccd.service.nonprod.TestCaseGenerationService.TEST_CASE_CREATION_NOT_SUPPORTED;

@ExtendWith(MockitoExtension.class)
class TestCaseGenerationServiceTest {

    @Mock
    private CaseSupportHelper caseSupportHelper;
    @Mock
    private TestCaseGenerationStrategy strategy1;
    @Mock
    private TestCaseGenerationStrategy strategy2;

    @InjectMocks
    private TestCaseGenerationService underTest;

    @Test
    void shouldGenerateCaseUsingFirstSupportingStrategy() throws Exception {
        // Given
        ReflectionTestUtils.setField(underTest, "testCaseGenerationStrategies",
                                     List.of(strategy1, strategy2));
        long caseReference = 123L;
        String label = "Test Case Creation";

        PCSCase fromEvent = mock(PCSCase.class);
        DynamicList dynamicList = mock(DynamicList.class);
        DynamicListElement selectedValue = mock(DynamicListElement.class);
        when(fromEvent.getNonProdSupportFileList()).thenReturn(dynamicList);
        Resource resource = mock(Resource.class);
        CaseSupportGenerationResponse expectedResponse = mock(CaseSupportGenerationResponse.class);

        when(fromEvent.getNonProdSupportFileList()).thenReturn(dynamicList);
        when(dynamicList.getValue()).thenReturn(selectedValue);
        when(selectedValue.getLabel()).thenReturn(label).thenReturn(label);

        when(strategy1.supports(label)).thenReturn(false);
        when(strategy2.supports(label)).thenReturn(true);

        when(caseSupportHelper.getNonProdResource(label)).thenReturn(resource);
        when(strategy2.generate(caseReference, fromEvent, resource)).thenReturn(expectedResponse);

        // When
        CaseSupportGenerationResponse result = underTest.caseGenerator(caseReference, fromEvent);

        // Then
        assertThat(result).isSameAs(expectedResponse);
        verify(strategy1).supports(label);
        verify(strategy2).supports(label);
        verify(caseSupportHelper).getNonProdResource(label);
        verify(strategy2).generate(caseReference, fromEvent, resource);
        verify(strategy1, never()).generate(anyLong(), any(), any());
    }

    @Test
    void shouldThrowSupportExceptionWhenDynamicListIsNull() throws IOException {
        // Given
        PCSCase fromEvent = mock(PCSCase.class);
        when(fromEvent.getNonProdSupportFileList()).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> underTest.caseGenerator(1L, fromEvent))
            .isInstanceOf(TestCaseSupportException.class)
            .hasMessage(TestCaseGenerationService.FAILED_TO_GENERATE_TEST_CASE)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .rootCause()
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(TestCaseGenerationService.NO_NON_PROD_CASE_AVAILABLE);

        verify(caseSupportHelper, never()).getNonProdResource(any());
        verify(strategy1, never()).supports(any());
        verify(strategy2, never()).supports(any());
    }

    @Test
    void shouldThrowSupportExceptionWhenNoStrategyFoundForLabel() throws IOException {
        // Given
        underTest = new TestCaseGenerationService(caseSupportHelper, List.of(strategy1, strategy2));

        long caseReference = 55L;
        String label = "Some Unknown Label";

        PCSCase fromEvent = mock(PCSCase.class);
        DynamicList dynamicList = mock(DynamicList.class);
        DynamicListElement selectedValue = mock(DynamicListElement.class);

        when(fromEvent.getNonProdSupportFileList()).thenReturn(dynamicList);
        when(dynamicList.getValue()).thenReturn(selectedValue);
        when(selectedValue.getLabel()).thenReturn(label);

        when(strategy1.supports(label)).thenReturn(false);
        when(strategy2.supports(label)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> underTest.caseGenerator(caseReference, fromEvent))
            .isInstanceOf(TestCaseSupportException.class)
            .hasMessage(FAILED_TO_GENERATE_TEST_CASE)
            .hasCauseInstanceOf(RuntimeException.class)
            .hasRootCauseMessage(TEST_CASE_CREATION_NOT_SUPPORTED + label);

        verify(strategy1).supports(label);
        verify(strategy2).supports(label);
        verify(caseSupportHelper, never()).getNonProdResource(any());
        verify(strategy1, never()).generate(anyLong(), any(), any());
        verify(strategy2, never()).generate(anyLong(), any(), any());
    }

    @Test
    void shouldWrapIOExceptionFromCaseSupportHelperInSupportException() throws Exception {
        // Given
        underTest = new TestCaseGenerationService(caseSupportHelper, List.of(strategy1));

        long caseReference = 9L;
        String label = "Create Something";

        PCSCase fromEvent = mock(PCSCase.class);
        DynamicList dynamicList = mock(DynamicList.class);
        DynamicListElement selectedValue = mock(DynamicListElement.class);

        when(fromEvent.getNonProdSupportFileList()).thenReturn(dynamicList);
        when(dynamicList.getValue()).thenReturn(selectedValue);
        when(selectedValue.getLabel()).thenReturn(label);

        when(strategy1.supports(label)).thenReturn(true);
        when(caseSupportHelper.getNonProdResource(label)).thenThrow(new IOException("boom"));

        // When / Then
        assertThatThrownBy(() -> underTest.caseGenerator(caseReference, fromEvent))
            .isInstanceOf(TestCaseSupportException.class)
            .hasMessage(FAILED_TO_GENERATE_TEST_CASE)
            .hasCauseInstanceOf(TestCaseSupportException.class)
            .extracting(Throwable::getCause)
            .satisfies(cause -> assertThat(cause.getCause()).isInstanceOf(IOException.class));

        verify(strategy1).supports(label);
        verify(caseSupportHelper).getNonProdResource(label);
        verify(strategy1, never()).generate(anyLong(), any(), any());
    }

    @Test
    void shouldWrapRuntimeExceptionThrownByStrategyGenerateInSupportException() throws Exception {
        // Given
        underTest = new TestCaseGenerationService(caseSupportHelper, List.of(strategy1));

        long caseReference = 77L;
        String label = "Create Something";

        PCSCase fromEvent = mock(PCSCase.class);
        DynamicList dynamicList = mock(DynamicList.class);
        DynamicListElement selectedValue = mock(DynamicListElement.class);
        Resource resource = mock(Resource.class);

        when(fromEvent.getNonProdSupportFileList()).thenReturn(dynamicList);
        when(dynamicList.getValue()).thenReturn(selectedValue);
        when(selectedValue.getLabel()).thenReturn(label);

        when(strategy1.supports(label)).thenReturn(true);
        when(caseSupportHelper.getNonProdResource(label)).thenReturn(resource);
        when(strategy1.generate(caseReference, fromEvent, resource))
            .thenThrow(new RuntimeException("generate failed"));

        // When / Then
        assertThatThrownBy(() -> underTest.caseGenerator(caseReference, fromEvent))
            .isInstanceOf(TestCaseSupportException.class)
            .hasMessage(FAILED_TO_GENERATE_TEST_CASE)
            .hasCauseInstanceOf(RuntimeException.class);

        verify(strategy1).supports(label);
        verify(caseSupportHelper).getNonProdResource(label);
        verify(strategy1).generate(caseReference, fromEvent, resource);
    }

}
