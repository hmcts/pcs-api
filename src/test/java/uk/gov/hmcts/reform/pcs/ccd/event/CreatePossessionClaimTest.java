package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnterPropertyAddress enterPropertyAddress;
    @Mock
    private CrossBorderPostcodeSelection crossBorderPostcodeSelection;

    private Event<PCSCase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        CreatePossessionClaim underTest
            = new CreatePossessionClaim(pcsCaseService, enterPropertyAddress, crossBorderPostcodeSelection);

        configuredEvent = getEvent(EventId.createPossessionClaim, buildEventConfig(underTest));
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        LegislativeCountry legislativeCountry = mock(LegislativeCountry.class);

        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(caseData.getLegislativeCountry()).thenReturn(legislativeCountry);

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(CASE_REFERENCE, caseData, null);

        // When
        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        // Then
        verify(pcsCaseService).createCase(CASE_REFERENCE, propertyAddress, legislativeCountry);
    }

}
