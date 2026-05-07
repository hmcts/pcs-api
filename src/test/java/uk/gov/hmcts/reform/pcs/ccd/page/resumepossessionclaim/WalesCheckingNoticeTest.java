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
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentSectionRoutingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalesCheckingNoticeTest extends BasePageTest {

    @Mock
    private WalesRentSectionRoutingService walesRentSectionRoutingService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new WalesCheckingNotice(walesRentSectionRoutingService));
    }

    @Test
    void shouldSetShowRentSectionPageToYesWhenRoutingServiceReturnsYes() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.YES);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getData().getShowRentSectionPage()).isEqualTo(YesOrNo.YES);
        verify(walesRentSectionRoutingService).shouldShowRentSection(caseData);
    }

    @Test
    void shouldSetShowRentSectionPageToNoWhenRoutingServiceReturnsNo() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.NO);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getData().getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
        verify(walesRentSectionRoutingService).shouldShowRentSection(caseData);
    }
}

