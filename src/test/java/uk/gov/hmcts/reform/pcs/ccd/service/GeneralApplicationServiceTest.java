package uk.gov.hmcts.reform.pcs.ccd.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@ExtendWith(MockitoExtension.class)
class GeneralApplicationServiceTest {

    private final GeneralApplicationService generalApplicationService = new GeneralApplicationService();

    @Mock
    private PCSCase pcsCase;

    @Test
    void shouldSetGeneralApplication() {

        // Test generalApplicationWanted field updates as expected
        assertGeneralApplicationField(
                pcsCase -> when(pcsCase.getGeneralApplicationWanted()).thenReturn(YesOrNo.YES),
                expected -> assertThat(expected.getGeneralApplicationWanted()).isTrue());
        assertGeneralApplicationField(
                pcsCase -> when(pcsCase.getGeneralApplicationWanted()).thenReturn(YesOrNo.NO),
                expected -> assertThat(expected.getGeneralApplicationWanted()).isFalse());
    }

    private void assertGeneralApplicationField(java.util.function.Consumer<PCSCase> setupMock,
            java.util.function.Consumer<GeneralApplication> assertions) {
        setupMock.accept(pcsCase);
        GeneralApplication actual = generalApplicationService.buildTGeneralApplication(pcsCase);
        assertions.accept(actual);
    }
}
