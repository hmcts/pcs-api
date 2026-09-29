package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManageFlagsTest extends BaseEventTest {
    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private ManageFlags underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(EventStates.amendFlags());
    }

    @Test
    void shouldCreateFlagsInSubmitCallback() {
        // Given
        List<ListValue<FlagDetail>> flagDetails = createFlagDetails();
        Flags flags = Flags.builder()
            .details(flagDetails)
            .build();

        PCSCase pcsCase = PCSCase.builder().caseFlags(flags).build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).patchCaseFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldUseCaseFlagsVersion2Point1UpdateJourney() {
        assertThat(getDisplayContextParameter("flagLauncherInternal"))
            .isEqualTo("#ARGUMENT(UPDATE,VERSION2.1)");
    }

    @Test
    void shouldConfigureBothInternalAndExternalPartyFlagCollections() {
        assertThat(getSubFieldIds("allDefendants"))
            .contains("defendantFlags", "partyFlagsExternal");
    }

    /**
     * Availability before the case is issued is a confirmed requirement, so it is asserted in its own
     * right and the whole state set is spelled out, rather than mirroring whichever helper the event
     * happens to call.
     */
    @Test
    void shouldBeAvailableBeforeTheCaseIsIssued() {
        assertThat(configuredEvent.getPreState())
            .contains(State.PENDING_CASE_ISSUED);
    }

    @Test
    void shouldBeAvailableInTheRequiredStatesAndNeverInDraft() {
        assertThat(configuredEvent.getPreState())
            .containsExactlyInAnyOrder(
                State.PENDING_CASE_ISSUED,
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.CASE_STAYED,
                State.BREATHING_SPACE,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME,
                State.ALL_FINAL_ORDERS_ISSUED,
                State.CLOSED)
            .doesNotContain(State.AWAITING_SUBMISSION_TO_HMCTS);
    }

    private List<ListValue<FlagDetail>> createFlagDetails() {

        return List.of(
            ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(FlagDetail.builder()
                           .flagCode("CF0002")
                           .name("Complex Case")
                           .build())
                .build());
    }
}
