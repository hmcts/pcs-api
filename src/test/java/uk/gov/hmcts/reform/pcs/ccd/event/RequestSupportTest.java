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
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagRoles.DEFENDANT_SUPPORT_REQUEST_ROLES;

@ExtendWith(MockitoExtension.class)
class RequestSupportTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private RequestSupport underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldPatchCaseFlagsInSubmitCallback() {
        // Given
        PartySupport partySupport = PartySupport.builder()
            .supportFlags(Flags.builder().details(createFlagDetails()).build())
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(ListValue.<PartySupport>builder()
                .id(UUID.randomUUID().toString())
                .value(partySupport)
                .build()))
            .build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).patchRequestedSupportFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldOfferOnlyEligibleDefendantPartiesInStartCallback() {
        UUID eligibleDefendantPartyId = UUID.randomUUID();
        UUID claimantPartyId = UUID.randomUUID();
        when(pcsCaseService.resolveEligibleDefendantPartyIds(TEST_CASE_REFERENCE))
            .thenReturn(Set.of(eligibleDefendantPartyId));

        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(partySupportFor(eligibleDefendantPartyId), partySupportFor(claimantPartyId)))
            .build();

        PCSCase result = callStartHandler(pcsCase);

        assertThat(result.getPartySupport())
            .extracting(ListValue::getId)
            .containsExactly(eligibleDefendantPartyId.toString());
    }

    @Test
    void shouldOfferNoPartiesInStartCallbackWhenNoDefendantIsRepresented() {
        when(pcsCaseService.resolveEligibleDefendantPartyIds(TEST_CASE_REFERENCE)).thenReturn(Set.of());

        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(partySupportFor(UUID.randomUUID())))
            .build();

        assertThat(callStartHandler(pcsCase).getPartySupport()).isEmpty();
    }

    @Test
    void shouldDiscardPartySupportEntriesWithoutAUsablePartyIdInStartCallback() {
        when(pcsCaseService.resolveEligibleDefendantPartyIds(TEST_CASE_REFERENCE))
            .thenReturn(Set.of(UUID.randomUUID()));

        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(
                ListValue.<PartySupport>builder().id(null).value(PartySupport.builder().build()).build(),
                ListValue.<PartySupport>builder().id("not-a-uuid").value(PartySupport.builder().build()).build()))
            .build();

        assertThat(callStartHandler(pcsCase).getPartySupport()).isEmpty();
    }

    @Test
    void shouldLeaveAbsentPartySupportUntouchedInStartCallback() {
        when(pcsCaseService.resolveEligibleDefendantPartyIds(TEST_CASE_REFERENCE)).thenReturn(Set.of());

        assertThat(callStartHandler(PCSCase.builder().build()).getPartySupport()).isNull();
    }

    private ListValue<PartySupport> partySupportFor(UUID partyId) {
        return ListValue.<PartySupport>builder()
            .id(partyId.toString())
            .value(PartySupport.builder().build())
            .build();
    }

    @Test
    void shouldUseExternalCreateJourney() {
        assertThat(getDisplayContextParameter("flagLauncherExternal"))
            .isEqualTo("#ARGUMENT(CREATE,EXTERNAL)");
    }

    @Test
    void shouldConfigureOnlyTheDedicatedSupportCollection() {
        assertThat(getSubFieldIds("partySupport"))
            .containsExactly("supportFlags");
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
                State.ALL_FINAL_ORDERS_ISSUED)
            .doesNotContain(State.AWAITING_SUBMISSION_TO_HMCTS);
    }

    @Test
    void shouldGrantEveryDefendantSidePersona() {
        assertThat(configuredEvent.getGrants().keySet())
            .containsAll(List.of(DEFENDANT_SUPPORT_REQUEST_ROLES));
        for (UserRole defendantRole : DEFENDANT_SUPPORT_REQUEST_ROLES) {
            assertThat(configuredEvent.getGrants().get(defendantRole))
                .containsExactlyInAnyOrderElementsOf(Permission.CRU);
        }
    }

    @Test
    void shouldNotGrantClaimantSideProfilesAnyExecution() {
        assertThat(configuredEvent.getGrants().keySet())
            .doesNotContain(UserRole.CLAIMANT,
                            UserRole.GA_CLAIMANT_SOLICITOR,
                            UserRole.CLAIMANT_SOLICITOR);
    }

    @Test
    void shouldNotGrantTheSharedLegacyProfessionalProfileExecution() {
        assertThat(configuredEvent.getGrants().keySet())
            .doesNotContain(UserRole.PCS_SOLICITOR);
    }

    @Test
    void shouldNotGrantCreateOrUpdateToAnyProfileOutsideTheDefendantSideSet() {
        assertThat(configuredEvent.getGrants().asMap())
            .allSatisfy((userRole, permissions) -> {
                if (permissions.contains(Permission.C) || permissions.contains(Permission.U)) {
                    assertThat(userRole).isIn(List.of(DEFENDANT_SUPPORT_REQUEST_ROLES));
                }
            });
    }

    @Test
    void shouldGrantHearingCentreRolesHistoryOnlyAccess() {
        assertThat(configuredEvent.getHistoryOnlyRoles())
            .contains(UserRole.HEARING_CENTRE_ADMIN.getRole(), UserRole.HEARING_CENTRE_TEAM_LEADER.getRole());
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_ADMIN))
            .containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_TEAM_LEADER))
            .containsExactly(Permission.R);
    }

    @Test
    void shouldNotConfigureALineSeparatorLabel() {
        assertThat(getEventFieldIds()).noneMatch(id -> id != null && id.contains("lineSeparator"));
    }

    private List<ListValue<FlagDetail>> createFlagDetails() {

        return List.of(
            ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(FlagDetail.builder()
                           .flagCode("RA0042")
                           .name("Reasonable adjustment")
                           .build())
                .build());
    }
}
