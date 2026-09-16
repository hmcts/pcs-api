package uk.gov.hmcts.reform.pcs.ccd.event.casedeletion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.TTL;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MarkCaseForDeletionTest extends BaseEventTest {

    private MarkCaseForDeletion underTest;

    @BeforeEach
    void setUp() {
        underTest = new MarkCaseForDeletion();
    }

    @Test
    void shouldConfigureDecentralisedWhenNonProdSupportEnabled() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .ttl(TTL.builder().build())
                .build();

        setEventUnderTest(underTest);

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        assertThat(submitResponse.getState()).isEqualTo(State.DRAFT_DISCARDED);
    }
}