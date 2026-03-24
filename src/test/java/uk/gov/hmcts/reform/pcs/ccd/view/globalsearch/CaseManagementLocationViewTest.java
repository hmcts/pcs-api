package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseManagementLocationViewTest {

    @Mock
    PCSCase pcsCase;


    private CaseManagementLocationView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseManagementLocationView();
    }

    @Test
    void shouldSetCaseManagementLocation() {

        when(pcsCase.getCaseManagementLocation()).thenReturn(29096);
        when(pcsCase.getRegionId()).thenReturn(1);

        underTest.setCaseManagementLocationField(pcsCase);

        // Then
        verify(pcsCase).setCaseManagementLocationFormatted("{region:1,baseLocation:29096}");
    }
}

