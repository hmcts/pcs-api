package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentSectionRoutingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckingNoticeTest extends BasePageTest {

    @Mock
    private RentSectionRoutingService rentSectionRoutingService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new CheckingNotice(rentSectionRoutingService));
    }

    @Test
    void shouldSetShowRentSectionPageToYesWhenRoutingServiceReturnsYes() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(rentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.YES);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getData().getShowRentSectionPage()).isEqualTo(YesOrNo.YES);
        verify(rentSectionRoutingService).shouldShowRentSection(caseData);
    }

    @Test
    void shouldSetShowRentSectionPageToNoWhenRoutingServiceReturnsNo() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(rentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.NO);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getData().getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
        verify(rentSectionRoutingService).shouldShowRentSection(caseData);
    }
}

