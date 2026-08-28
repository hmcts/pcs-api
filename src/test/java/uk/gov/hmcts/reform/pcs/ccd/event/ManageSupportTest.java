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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagRoles.EXTERNAL_CASE_FLAG_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventStates.manageSupport;

@ExtendWith(MockitoExtension.class)
class ManageSupportTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private ManageSupport underTest;

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
        verify(pcsCaseService).patchSupportFlags(TEST_CASE_REFERENCE, pcsCase.getPartySupport());
    }

    @Test
    void shouldUseExternalUpdateJourney() {
        assertThat(getDisplayContextParameter("flagLauncherExternal"))
            .isEqualTo("#ARGUMENT(UPDATE,EXTERNAL)");
    }

    @Test
    void shouldConfigureOnlyTheDedicatedSupportCollection() {
        assertThat(getSubFieldIds("partySupport"))
            .containsExactly("supportFlags");
    }

    @Test
    void shouldBeAvailableFromPendingCaseIssuedOnwards() {
        assertThat(configuredEvent.getPreState())
            .containsExactlyInAnyOrder(manageSupport())
            .doesNotContain(State.AWAITING_SUBMISSION_TO_HMCTS);
    }

    @Test
    void shouldGrantEveryExternalPersona() {
        assertThat(configuredEvent.getGrants().keySet())
            .containsAll(List.of(EXTERNAL_CASE_FLAG_ROLES));
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
                           .status("Inactive")
                           .flagUpdateComment("No longer needed")
                           .build())
                .build());
    }
}
