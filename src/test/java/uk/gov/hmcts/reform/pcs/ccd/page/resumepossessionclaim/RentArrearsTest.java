package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RentArrearsTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrears(textAreaValidationService));
    }

    @Test
    void shouldValidateRentArrearsRecoveryAttemptDetailsInput() {
        // Given
        String rentRecoveryAttempt = "rent recovery attempt";
        String label = "Give details of previous steps taken to recover rent arrears";
        Integer characterLimit = 500;
        PCSCase caseData = PCSCase.builder()
            .rentArrears(
                RentArrearsSection
                    .builder()
                    .recoveryAttemptDetails(rentRecoveryAttempt)
                    .build()
            ).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
        verify(textAreaValidationService, times(1))
            .validateSingleTextArea(eq(rentRecoveryAttempt), eq(label), eq(characterLimit));
    }
}
