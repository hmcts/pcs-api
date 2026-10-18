package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision.ACCEPT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision.REJECT;

@ExtendWith(MockitoExtension.class)
class DecideEnforcementSubmitHandlerTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String DASHED_CASE_REFERENCE = "1234-5678-9012-3456";
    private static final AddressUK PROPERTY_ADDRESS = AddressUK.builder().addressLine1("1 High Street").build();
    private static final String CASE_NAME = "Landlord Ltd vs John Smith";

    @Mock
    private EnforcementDecisionService enforcementDecisionService;
    @Mock
    private AddressFormatter addressFormatter;
    @Spy
    private CaseReferenceFormatter caseReferenceFormatter;
    @InjectMocks
    private DecideEnforcementSubmitHandler underTest;

    @ParameterizedTest
    @CsvSource({
        "WARRANT, WP-00000001, Warrant of possession approved, Warrant number: WP-00000001, allocated to a bailiff",
        "WARRANT_OF_RESTITUTION, WR-00000002, Warrant of restitution approved, Warrant number: WR-00000002,"
            + " allocated to a bailiff",
        "WRIT, HP-00000003, Writ of possession approved, Writ number: HP-00000003, High Court Enforcement Officer",
        "WRIT_OF_RESTITUTION, HR-00000004, Writ of restitution approved, Writ number: HR-00000004,"
            + " High Court Enforcement Officer",
    })
    void shouldApproveTheApplicationAndConfirmIt(SelectEnforcementType type, String number, String title,
                                                 String numberLine, String whatNext) {
        when(enforcementDecisionService.accept(CASE_REFERENCE))
            .thenReturn(Optional.of(new EnforcementDecisionOutcome(type, ACCEPT, number)));
        givenFormattedAddress();

        SubmitResponse<State> response = underTest.submit(payload(ACCEPT, null));

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getConfirmationHeader()).isEqualTo("# " + title);
        assertThat(response.getConfirmationBody())
            .contains(numberLine, "Case number: " + DASHED_CASE_REFERENCE, "Property address: 1 High Street",
                      CASE_NAME, whatNext);
        verify(enforcementDecisionService, never()).reject(anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest
    @CsvSource({
        "WARRANT, Warrant of possession rejected",
        "WARRANT_OF_RESTITUTION, Warrant of restitution rejected",
        "WRIT, Writ of possession rejected",
        "WRIT_OF_RESTITUTION, Writ of restitution rejected",
    })
    void shouldRejectTheApplicationAndPutTheCaseBackSoTheClaimantCanApplyAgain(SelectEnforcementType type,
                                                                               String title) {
        when(enforcementDecisionService.reject(CASE_REFERENCE, "No possession order"))
            .thenReturn(Optional.of(new EnforcementDecisionOutcome(type, REJECT, null)));
        givenFormattedAddress();

        SubmitResponse<State> response = underTest.submit(payload(REJECT, "No possession order"));

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
        assertThat(response.getConfirmationHeader()).isEqualTo("# " + title);
        assertThat(response.getConfirmationBody())
            .contains("Case number: " + DASHED_CASE_REFERENCE, "Property address: 1 High Street", CASE_NAME,
                      "The claimant can apply again.")
            .doesNotContain("Warrant number", "Writ number");
        verify(enforcementDecisionService, never()).accept(anyLong());
    }

    @Test
    void shouldRefuseASubmissionWithNoDecision() {
        SubmitResponse<State> response = underTest.submit(payload(null, null));

        assertThat(response.getErrors()).containsExactly(DecideEnforcementSubmitHandler.NO_DECISION_MESSAGE);
        assertThat(response.getState()).isNull();
        verifyNoInteractions(enforcementDecisionService);
    }

    @Test
    void shouldRefuseASubmissionWithNoEnforcementOrder() {
        EventPayload<PCSCase, State> payload = new EventPayload<>(CASE_REFERENCE, PCSCase.builder().build(), null);

        SubmitResponse<State> response = underTest.submit(payload);

        assertThat(response.getErrors()).containsExactly(DecideEnforcementSubmitHandler.NO_DECISION_MESSAGE);
        assertThat(response.getState()).isNull();
        verifyNoInteractions(enforcementDecisionService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRefuseARejectionWithNoReasonWithoutStartingATransaction(String reason) {
        SubmitResponse<State> response = underTest.submit(payload(REJECT, reason));

        assertThat(response.getErrors())
            .containsExactly(EnforcementDecisionService.NO_REJECTION_REASON_MESSAGE);
        assertThat(response.getState()).isNull();
        verifyNoInteractions(enforcementDecisionService);
    }

    @Test
    void shouldReportAnApplicationAlreadyDecidedByAnotherCaseworkerAsAnError() {
        when(enforcementDecisionService.accept(CASE_REFERENCE)).thenReturn(Optional.empty());

        SubmitResponse<State> response = underTest.submit(payload(ACCEPT, null));

        assertThat(response.getErrors()).containsExactly(NoEnforcementAwaitingReviewException.MESSAGE);
        assertThat(response.getState()).isNull();
        assertThat(response.getConfirmationBody()).isNull();
        verifyNoInteractions(addressFormatter);
    }

    @Test
    void shouldReportARejectionOfAnApplicationAlreadyDecidedAsAnError() {
        when(enforcementDecisionService.reject(CASE_REFERENCE, "No possession order")).thenReturn(Optional.empty());

        SubmitResponse<State> response = underTest.submit(payload(REJECT, "No possession order"));

        assertThat(response.getErrors()).containsExactly(NoEnforcementAwaitingReviewException.MESSAGE);
        assertThat(response.getState()).isNull();
        verifyNoInteractions(addressFormatter);
    }

    private void givenFormattedAddress() {
        when(addressFormatter.formatMediumAddress(PROPERTY_ADDRESS, COMMA_DELIMITER)).thenReturn("1 High Street");
    }

    private static EventPayload<PCSCase, State> payload(EnforcementDecision decision, String reason) {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(PROPERTY_ADDRESS)
            .caseNameHmctsInternal(CASE_NAME)
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementDecision(decision)
                                  .enforcementRejectionReason(reason)
                                  .build())
            .build();
        return new EventPayload<>(CASE_REFERENCE, caseData, null);
    }
}
