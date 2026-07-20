package uk.gov.hmcts.reform.pcs.ccd.event.deletecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.deletecase.DeleteCasePageConfigurer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteThisCaseTest extends BaseEventTest {

    @InjectMocks
    private DeleteThisCase underTest;
    @Mock
    private DeleteCasePageConfigurer deleteCasePageConfigurer;

    @Test
    void shouldConfigureDecentralisedWhenNonProdSupportEnabled() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .deleteUnsubmittedClaim(YesOrNo.YES)
                .build();

        setEventUnderTest(underTest);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(deleteCasePageConfigurer).configurePages(any(PageBuilder.class));
    }
}