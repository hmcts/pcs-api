package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;

@ExtendWith(MockitoExtension.class)
class DecideEnforcementStartHandlerTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private EnforcementDecisionService enforcementDecisionService;
    @Mock
    private AddressFormatter addressFormatter;
    @InjectMocks
    private DecideEnforcementStartHandler underTest;

    @Test
    void shouldSetTheHeaderAndClearAnEarlierDecision() {
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 High Street").build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .caseNameHmctsInternal("Landlord Ltd vs John Smith")
            .enforcementOrder(EnforcementOrder.builder()
                                  .enforcementDecision(EnforcementDecision.REJECT)
                                  .enforcementRejectionReason("old reason")
                                  .build())
            .build();
        when(enforcementDecisionService.hasEnforcementAwaitingReview(CASE_REFERENCE)).thenReturn(true);
        when(addressFormatter.formatMediumAddress(propertyAddress, BR_DELIMITER)).thenReturn("1 High Street<br>");

        PCSCase result = underTest.start(payload(caseData));

        EnforcementOrder enforcementOrder = result.getEnforcementOrder();
        assertThat(enforcementOrder.getFormattedPropertyAddress()).isEqualTo("1 High Street<br>");
        assertThat(enforcementOrder.getFormattedCaseName()).isEqualTo("Landlord Ltd vs John Smith");
        assertThat(enforcementOrder.getEnforcementDecision()).isNull();
        assertThat(enforcementOrder.getEnforcementRejectionReason()).isNull();
    }

    @Test
    void shouldCreateTheEnforcementOrderWhenTheCaseHasNone() {
        PCSCase caseData = PCSCase.builder().build();
        when(enforcementDecisionService.hasEnforcementAwaitingReview(CASE_REFERENCE)).thenReturn(true);

        assertThat(underTest.start(payload(caseData)).getEnforcementOrder()).isNotNull();
    }

    @Test
    void shouldRefuseToStartWhenNoApplicationIsAwaitingReview() {
        when(enforcementDecisionService.hasEnforcementAwaitingReview(CASE_REFERENCE)).thenReturn(false);
        EventPayload<PCSCase, State> payload = payload(PCSCase.builder().build());

        assertThatThrownBy(() -> underTest.start(payload))
            .isInstanceOf(NoEnforcementAwaitingReviewException.class)
            .hasMessage("There is no warrant or writ application waiting for a decision on this case");
        verifyNoInteractions(addressFormatter);
    }

    private static EventPayload<PCSCase, State> payload(PCSCase caseData) {
        return new EventPayload<>(CASE_REFERENCE, caseData, null);
    }
}
