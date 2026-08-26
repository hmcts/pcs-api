package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.SupportReviewService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.SupportReviewRoles.SUPPORT_REVIEW_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventStates.reviewSupportRequest;

@ExtendWith(MockitoExtension.class)
class ReviewSupportRequestTest extends BaseEventTest {

    private static final String PARTY_ID = UUID.randomUUID().toString();

    @Mock
    private PcsCaseService pcsCaseService;

    @Spy
    private SupportReviewService supportReviewService;

    @InjectMocks
    private ReviewSupportRequest underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldPatchReviewedSupportFlagsInSubmitCallback() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).patchReviewedSupportFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldProjectOnlyRequestedSupportFlagsOnStart() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .partySupport(List.of(ListValue.<PartySupport>builder()
                .id(PARTY_ID)
                .value(PartySupport.builder()
                    .supportFlags(Flags.builder()
                        .visibility(FlagVisibility.EXTERNAL)
                        .details(createMixedStatusFlagDetails())
                        .build())
                    .build())
                .build()))
            .build();

        // When
        PCSCase result = callStartHandler(pcsCase);

        // Then
        assertThat(result.getSupportReviewFlags()).hasSize(1);
        assertThat(result.getSupportReviewFlags().getFirst().getValue().getSupportFlags().getDetails())
            .extracting(detail -> detail.getValue().getStatus())
            .containsExactly("Requested");
    }

    @Test
    void shouldBindTheRequestedSupportCollectionRatherThanTheGeneralFlagCollections() {
        assertThat(getEventFieldIds())
            .contains("supportReviewFlags")
            .doesNotContain("caseFlags", "parties", "allDefendants", "partySupport");
    }

    @Test
    void shouldUseCaseFlagsVersion2Point1UpdateJourney() {
        assertThat(getDisplayContextParameter("flagLauncherInternal"))
            .isEqualTo("#ARGUMENT(UPDATE,VERSION2.1)");
    }

    @Test
    void shouldNotExposeTheExternalFlagLauncher() {
        assertThat(getEventFieldIds())
            .contains("flagLauncherInternal")
            .doesNotContain("flagLauncherExternal");
    }

    @Test
    void shouldExposeTheSupportFlagsSubFieldForReview() {
        assertThat(getSubFieldIds("supportReviewFlags"))
            .contains("supportFlags");
    }

    @Test
    void shouldBeAvailableFromPendingCaseIssuedOnwards() {
        assertThat(configuredEvent.getPreState())
            .containsExactlyInAnyOrder(reviewSupportRequest())
            .doesNotContain(State.AWAITING_SUBMISSION_TO_HMCTS);
    }

    @Test
    void shouldGrantReviewAccessToEveryInternalSupportReviewRole() {
        for (UserRole role : SUPPORT_REVIEW_ROLES) {
            assertThat(configuredEvent.getGrants().get(role))
                .as("grants for %s", role)
                .containsAll(Permission.CRU);
        }
    }

    @Test
    void shouldNotGrantReviewAccessToExternalUsers() {
        assertThat(configuredEvent.getGrants().get(UserRole.CITIZEN)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.DEFENDANT)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.DEFENDANT_SOLICITOR)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.CLAIMANT_SOLICITOR)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.PCS_SOLICITOR)).isEmpty();
    }

    @Test
    void shouldNotAllowJudicialUsersToUpdateFlags() {
        assertThat(configuredEvent.getGrants().get(UserRole.JUDGE))
            .containsExactly(Permission.R);
    }

    private List<ListValue<FlagDetail>> createMixedStatusFlagDetails() {

        return List.of(
            ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(FlagDetail.builder()
                           .flagCode("RA0042")
                           .name("Sign Language Interpreter")
                           .status("Requested")
                           .build())
                .build(),
            ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(FlagDetail.builder()
                           .flagCode("RA0013")
                           .name("Assistance dog")
                           .status("Active")
                           .build())
                .build());
    }
}
