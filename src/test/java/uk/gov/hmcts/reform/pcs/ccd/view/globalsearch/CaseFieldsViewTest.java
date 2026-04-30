package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.sdk.type.CaseLocation;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseFieldsViewTest {

    private static final int CASE_MANAGEMENT_LOCATION_NUMBER = 29096;

    @Mock
    private PCSCase pcsCase;
    @Mock
    private CaseNameFormatter caseNameFormatter;

    private CaseFieldsView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFieldsView(caseNameFormatter);
        ReflectionTestUtils.setField(underTest, "caseManagementCategory", "Property Possession Claims");
    }

    @Test
    void shouldSetCaseName() {
        // Given
        String expectedCaseName = "formatted case name";
        when(caseNameFormatter.formatCaseName(pcsCase)).thenReturn(expectedCaseName);

        //When
        underTest.setCaseFields(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted(expectedCaseName);
        verify(pcsCase).setCaseNameHmctsInternal(expectedCaseName);
        verify(pcsCase).setCaseNamePublic(expectedCaseName);
    }

    @Test
    void shouldSetCaseManagementLocation() {

        //Given
        when(pcsCase.getCaseManagementLocationNumber()).thenReturn(CASE_MANAGEMENT_LOCATION_NUMBER);
        when(pcsCase.getRegionId()).thenReturn(1);

        //When
        underTest.setCaseFields(pcsCase);

        // Then
        verify(pcsCase).setCaseManagementLocation(CaseLocation.builder()
            .baseLocation(String.valueOf(CASE_MANAGEMENT_LOCATION_NUMBER))
            .region("1")
            .build());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "2096,null",
        "null,1",
        "null,null"
    }, nullValues = {"null"})
    void shouldNotCallSetCaseManagementLocationWhenEitherIdIsNull(Integer epimsId, Integer regionId) {

        //Given
        when(pcsCase.getCaseManagementLocationNumber()).thenReturn(epimsId);
        when(pcsCase.getRegionId()).thenReturn(regionId);

        //When
        underTest.setCaseFields(pcsCase);

        // Then
        verify(pcsCase, never()).setCaseManagementLocation(any(CaseLocation.class));
    }

    @Test
    void shouldSetCaseManagementCategory() {
        ArgumentCaptor<DynamicList> captor = ArgumentCaptor.forClass(DynamicList.class);

        //When
        underTest.setCaseFields(pcsCase);

        // Then
        verify(pcsCase).setCaseManagementCategory(captor.capture());
        DynamicList result = captor.getValue();
        assertThat(result.getValue().getLabel()).isEqualTo("Property Possession Claims");
        assertThat(result.getListItems()).hasSize(1);
        assertThat(result.getListItems().getFirst().getLabel()).isEqualTo("Property Possession Claims");
    }
}
