package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private static final AddressUK PROPERTY_ADDRESS = AddressUK.builder().addressLine1("1 High Street").build();
    private static final String CASE_NAME = "Landlord Ltd vs John Smith";

    @Mock
    private EnforcementDecisionService enforcementDecisionService;
    @Mock
    private AddressFormatter addressFormatter;
    @InjectMocks
    private DecideEnforcementSubmitHandler underTest;

    @ParameterizedTest
    @CsvSource({
        "WARRANT, WP-00000001, Warrant approved, Warrant number: WP-00000001, allocated to a bailiff",
        "WARRANT_OF_RESTITUTION, WR-00000002, Warrant approved, Warrant number: WR-00000002, allocated to a bailiff",
        "WRIT, HP-00000003, Writ approved, Writ number: HP-00000003, transferred to the High Court",
        "WRIT_OF_RESTITUTION, HR-00000004, Writ approved, Writ number: HR-00000004, transferred to the High Court",
    })
    void shouldApproveTheApplicationAndConfirmIt(SelectEnforcementType type, String number, String title,
                                                 String numberLine, String whatNext) {
        when(enforcementDecisionService.accept(CASE_REFERENCE))
            .thenReturn(new EnforcementDecisionOutcome(type, ACCEPT, number));
        givenFormattedAddress();

        SubmitResponse<State> response = underTest.submit(payload(ACCEPT, null));

        assertThat(response.getState()).isNull();
        assertThat(response.getConfirmationBody())
            .contains(title, numberLine, "Case number: " + CASE_REFERENCE, "Property address: 1 High Street",
                      CASE_NAME, whatNext);
        verify(enforcementDecisionService, never()).reject(anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest
    @CsvSource({
        "WARRANT, Warrant rejected",
        "WARRANT_OF_RESTITUTION, Warrant rejected",
        "WRIT, Writ rejected",
        "WRIT_OF_RESTITUTION, Writ rejected",
    })
    void shouldRejectTheApplicationAndPutTheCaseBackSoTheClaimantCanApplyAgain(SelectEnforcementType type,
                                                                               String title) {
        when(enforcementDecisionService.reject(CASE_REFERENCE, "No possession order"))
            .thenReturn(new EnforcementDecisionOutcome(type, REJECT, null));
        givenFormattedAddress();

        SubmitResponse<State> response = underTest.submit(payload(REJECT, "No possession order"));

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
        assertThat(response.getConfirmationBody())
            .contains(title, "Case number: " + CASE_REFERENCE, "Property address: 1 High Street", CASE_NAME,
                      "The claimant can apply again.")
            .doesNotContain("number: WP", "number: HP", "approved");
        verify(enforcementDecisionService, never()).accept(anyLong());
    }

    @Test
    void shouldRefuseASubmissionWithNoDecision() {
        EventPayload<PCSCase, State> payload = payload(null, null);

        assertThatThrownBy(() -> underTest.submit(payload))
            .isInstanceOf(EnforcementEventNotAvailableException.class);
        verifyNoInteractions(enforcementDecisionService);
    }

    @Test
    void shouldRefuseASubmissionWithNoEnforcementOrder() {
        EventPayload<PCSCase, State> payload = new EventPayload<>(CASE_REFERENCE, PCSCase.builder().build(), null);

        assertThatThrownBy(() -> underTest.submit(payload))
            .isInstanceOf(EnforcementEventNotAvailableException.class);
        verifyNoInteractions(enforcementDecisionService);
    }

    @Test
    void shouldOnlyChangeTheCaseStateOnRejection() {
        assertThat(DecideEnforcementSubmitHandler.stateAfter(REJECT)).isEqualTo(State.CASE_ISSUED);
        assertThat(DecideEnforcementSubmitHandler.stateAfter(ACCEPT)).isNull();
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
