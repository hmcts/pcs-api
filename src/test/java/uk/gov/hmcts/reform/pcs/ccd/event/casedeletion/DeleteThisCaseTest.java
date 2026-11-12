package uk.gov.hmcts.reform.pcs.ccd.event.casedeletion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.TTL;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DeleteThisCaseTest extends BaseEventTest {

    private DeleteThisCase underTest;

    @BeforeEach
    void setUp() {
        underTest = new DeleteThisCase();
    }

    @Test
    void shouldConfigureDecentralised() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .ttl(TTL.builder().build())
                .build();

        setEventUnderTest(underTest);

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        assertThat(submitResponse.getState()).isNull();
    }

    @Test
    void shouldReturnDraftDiscardedStateWhenDeleteDraftClaimIsYes() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .ttl(TTL.builder().build())
                .deleteDraftClaim(YesOrNo.YES)
                .build();

        setEventUnderTest(underTest);

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        assertThat(submitResponse.getState()).isEqualTo(State.DRAFT_DISCARDED);
        assertThat(submitResponse.getConfirmationBody()).contains("Case deleted");
        assertThat(submitResponse.getConfirmationBody()).contains("Case number: " + TEST_CASE_REFERENCE);
    }

    @Test
    void shouldReturnDefaultResponseWhenDeleteDraftClaimIsNo() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .ttl(TTL.builder().build())
                .deleteDraftClaim(YesOrNo.NO)
                .build();

        setEventUnderTest(underTest);

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        assertThat(submitResponse.getState()).isNull();
    }
}
