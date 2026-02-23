package uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionsclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespondToPossessionDraftSavePageTest extends BasePageTest {

    @Mock
    private ImmutablePartyFieldValidator immutableFieldValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RespondToPossessionDraftSavePage(immutableFieldValidator));
    }

    @Test
    void shouldReturnPartialUpdateWhenNoViolations() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                                         .defendantContactDetails(DefendantContactDetails.builder()
                                                                      .party(Party.builder()
                                                                                 .firstName("Jack")
                                                                                 .lastName("Smith")
                                                                                 .build())
                                                                      .build())
                                         .defendantResponses(DefendantResponses.builder()
                                                                 .noticeReceived(YesNoNotSure.NO)
                                                                 .build())
                                         .build())
            .build();

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getPossessionClaimResponse().getDefendantContactDetails())
            .isEqualTo(caseData.getPossessionClaimResponse().getDefendantContactDetails());
        assertThat(response.getData().getPossessionClaimResponse().getDefendantResponses())
            .isEqualTo(caseData.getPossessionClaimResponse().getDefendantResponses());
    }

    @Test
    void shouldReturnErrorsWhenImmutableFieldViolationsFound() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                                         .defendantContactDetails(DefendantContactDetails.builder()
                                                                      .party(Party.builder()
                                                                                 .firstName("Jack")
                                                                                 .lastName("Smith")
                                                                                 .build())
                                                                      .build())
                                         .defendantResponses(DefendantResponses.builder()
                                                                 .noticeReceived(YesNoNotSure.NO)
                                                                 .build())
                                         .build())
            .build();

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of("firstName", "lastName"));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            "Invalid submission: immutable field must not be sent: firstName",
            "Invalid submission: immutable field must not be sent: lastName"
        );
        assertThat(response.getData()).isNull();
    }

    @Test
    void shouldSkipValidationWhenPartyIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                                         .defendantContactDetails(DefendantContactDetails.builder()
                                                                      .party(null)
                                                                      .build())
                                         .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
    }
}
