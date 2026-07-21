package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.CaseLocation;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.hearing.ConfirmationBodyRenderer;
import uk.gov.hmcts.reform.pcs.ccd.event.hearing.ManageHearing;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingSummaryRenderer;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Month.JULY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
public class ManageHearingTest extends BaseEventTest {

    private static final int BASE_LOCATION_ID = 1;
    private static final LocalDateTime FIXED_TEST_TIME = LocalDate.of(2026, JULY, 10).atTime(10, 20);

    @Mock
    private ManageHearingConfigurer manageHearingConfigurer;
    @Mock
    private HearingService hearingService;
    @Mock
    private LocationReferenceService locationReferenceService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private HearingSummaryRenderer hearingSummaryRenderer;
    @Mock
    private ConfirmationBodyRenderer confirmationBodyRenderer;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaim;
    @Mock(strictness = LENIENT)
    private Clock ukClock;

    @InjectMocks
    private ManageHearing underTest;

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(FIXED_TEST_TIME.atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);

        underTest = new ManageHearing(manageHearingConfigurer, hearingService, locationReferenceService,
                                      pcsCaseService, partyService, hearingSummaryRenderer, confirmationBodyRenderer,
                                      ukClock);
        setEventUnderTest(underTest);
    }

    @Test
    void shouldConfigurePages() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(manageHearingConfigurer).configurePages(any(PageBuilder.class));
    }

    @Nested
    @DisplayName("Start Handler")
    class StartHandlerTests {

        @Mock(strictness = LENIENT)
        private CaseLocation caseLocation;

        @BeforeEach
        void setUp() {
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
            when(pcsCaseEntity.getMainClaim()).thenReturn(mainClaim);
            when(caseLocation.getBaseLocation()).thenReturn(Integer.toString(BASE_LOCATION_ID));
        }

        @ParameterizedTest
        @MethodSource("hearingScenarios")
        void shouldSetShowManageHearingFlags(List<HearingEntity> hearingEntities,
                                             VerticalYesNo expectedShowManageHearingPage,
                                             ManageHearingOption expectedManageHearingOption) {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearing(new Hearing())
                .build();

            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
            when(pcsCaseEntity.getHearings()).thenReturn(hearingEntities);

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getShowManageHearingPage()).isEqualTo(expectedShowManageHearingPage);
            assertThat(response.getManageHearingOption()).isEqualTo(expectedManageHearingOption);
        }

        private static Stream<Arguments> hearingScenarios() {
            return Stream.of(
                arguments(
                    List.of(),
                    VerticalYesNo.NO,
                    ManageHearingOption.ADD),
                arguments(
                    List.of(createHearing(FIXED_TEST_TIME.minusSeconds(1))),
                    VerticalYesNo.NO,
                    ManageHearingOption.ADD
                ),
                arguments(
                    List.of(createCancelledHearing(FIXED_TEST_TIME.plusSeconds(1))),
                    VerticalYesNo.NO,
                    ManageHearingOption.ADD
                ),
                arguments(
                    List.of(createHearing(FIXED_TEST_TIME.plusSeconds(1))),
                    VerticalYesNo.YES,
                    null
                ),
                arguments(
                    List.of(
                        createCancelledHearing(FIXED_TEST_TIME.plusSeconds(1)),
                        createHearing(FIXED_TEST_TIME.plusSeconds(2))),
                    VerticalYesNo.YES,
                    null
                )
            );
        }

        @Test
        void shouldSetPartyList() {
            // Given
            UUID claimantId = UUID.randomUUID();
            PartyEntity claimantParty = createParty(claimantId);
            when(partyService.getPartyName(claimantParty)).thenReturn("claimant name");
            when(partyService.getPartyLabel(mainClaim, claimantId)).thenReturn("claimant label");

            UUID defendantId = UUID.randomUUID();
            PartyEntity defendantParty = createParty(defendantId);
            when(partyService.getPartyName(defendantParty)).thenReturn("defendant name");
            when(partyService.getPartyLabel(mainClaim, defendantId)).thenReturn("defendant label");

            when(mainClaim.getClaimParties()).thenReturn(List.of(
                createClaimParty(PartyRole.CLAIMANT, claimantParty),
                createClaimParty(PartyRole.DEFENDANT, defendantParty)
            ));

            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            DynamicMultiSelectList partyMultiSelectionList = response.getPartyMultiSelectionList();
            assertThat(partyMultiSelectionList).isNotNull();

            List<DynamicListElement> listItems = partyMultiSelectionList.getListItems();
            assertThat(listItems).hasSize(2);
            assertThat(listItems.getFirst().getCode()).isEqualTo(claimantId);
            assertThat(listItems.getFirst().getLabel()).isEqualTo("claimant name - claimant label");
            assertThat(listItems.getLast().getCode()).isEqualTo(defendantId);
            assertThat(listItems.getLast().getLabel()).isEqualTo("defendant name - defendant label");
        }

        @Test
        void shouldSetHearingLocation() {
            // Given
            String expectedHearingLocation = "Hearing location name";

            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .build();

            stubHearingLocation(expectedHearingLocation);

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getHearingLocation()).isEqualTo(expectedHearingLocation);
        }

        @Test
        void shouldSetNextHearingId() {
            // Given
            HearingEntity hearingEntity1 = createHearing(FIXED_TEST_TIME.minusSeconds(1));
            hearingEntity1.setId(1001L);

            HearingEntity hearingEntity2 = createHearing(FIXED_TEST_TIME.plusSeconds(1));
            hearingEntity2.setId(1002L);

            HearingEntity hearingEntity3 = createHearing(FIXED_TEST_TIME.plusSeconds(2));
            hearingEntity3.setId(1003L);

            HearingEntity hearingEntity4 = createHearing(FIXED_TEST_TIME.plusSeconds(3));
            hearingEntity4.setId(1004L);

            when(pcsCaseEntity.getHearings())
                .thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3, hearingEntity4));

            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearing(new Hearing())
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getHearing().getHearingId()).isEqualTo(1002L);
        }

        @Test
        void shouldSetHearingSummaryMarkdown() {
            // Given
            final String hearingLocation = "Hearing location name";
            final String expectedHearingSummaryMarkdown = "some hearing summary markdown";

            stubHearingLocation(hearingLocation);

            HearingEntity hearingEntity = createHearing(FIXED_TEST_TIME.plusSeconds(1));

            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
            when(pcsCaseEntity.getHearings()).thenReturn(List.of(hearingEntity));
            when(hearingSummaryRenderer.renderMarkdown(hearingEntity, hearingLocation))
                .thenReturn(expectedHearingSummaryMarkdown);

            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearing(new Hearing())
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getHearing().getHearingSummaryMarkdown()).isEqualTo(expectedHearingSummaryMarkdown);
        }

        @Test
        void shouldHandleNoCaseManagementLocation() {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getHearingLocation()).isEqualTo("Unable to find hearing location");
        }

        @Test
        void shouldHandleExceptionGettingHearingVenue() {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .build();

            when(locationReferenceService.getCourtVenues(List.of(BASE_LOCATION_ID))).thenThrow(new RuntimeException());

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getHearingLocation()).isEqualTo("Unable to find hearing location");
        }

        @Test
        void shouldHandleNoResultGettingHearingVenue() {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .build();

            when(locationReferenceService.getCourtVenues(List.of(BASE_LOCATION_ID))).thenReturn(List.of());

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getHearingLocation()).isEqualTo("Unable to find hearing location");
        }

        private static HearingEntity createHearing(LocalDateTime hearingDate) {
            return HearingEntity.builder()
                .hearingDate(hearingDate)
                .build();
        }

        private static HearingEntity createCancelledHearing(LocalDateTime hearingDate) {
            return HearingEntity.builder()
                .hearingDate(hearingDate)
                .cancelled(true)
                .build();
        }

        private void stubHearingLocation(String expectedHearingLocation) {
            CourtVenue courtVenue = mock(CourtVenue.class);
            when(locationReferenceService.getCourtVenues(List.of(BASE_LOCATION_ID))).thenReturn(List.of(courtVenue));
            when(courtVenue.courtName()).thenReturn(expectedHearingLocation);
        }

    }

    @Nested
    @DisplayName("Submit Handler")
    class SubmitHandlerTests {

        @Test
        void shouldAddHearingOnSubmit() {
            // Given
            AddressUK address = AddressUK.builder().build();

            PCSCase pcsCase = PCSCase.builder()
                .propertyAddress(address)
                .manageHearingOption(ManageHearingOption.ADD)
                .caseNameHmctsInternal("Claimant v Defendant")
                .build();

            String expectedConfirmationBody = "confirmation body";
            when(confirmationBodyRenderer.renderHearingAddedConfirmationBody(pcsCase, TEST_CASE_REFERENCE))
                .thenReturn(expectedConfirmationBody);

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

            // Then
            verify(hearingService).addHearing(TEST_CASE_REFERENCE, pcsCase);
            assertThat(submitResponse.getConfirmationBody()).isEqualTo(expectedConfirmationBody);
        }

        @Test
        void shouldAddHearingOnSubmitWhenMangeHearingPageWasNotShown() {
            // Given
            AddressUK address = AddressUK.builder().build();

            PCSCase pcsCase = PCSCase.builder()
                .propertyAddress(address)
                .caseNameHmctsInternal("Claimant v Defendant")
                .showManageHearingPage(VerticalYesNo.NO)
                .build();

            String expectedConfirmationBody = "confirmation body";
            when(confirmationBodyRenderer.renderHearingAddedConfirmationBody(pcsCase, TEST_CASE_REFERENCE))
                .thenReturn(expectedConfirmationBody);

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

            // Then
            verify(hearingService).addHearing(TEST_CASE_REFERENCE, pcsCase);
            assertThat(submitResponse.getConfirmationBody()).isEqualTo(expectedConfirmationBody);
        }

        @Test
        void shouldNotAddHearingWhenManageHearingOptionIsNotAdd() {
            // Given
            AddressUK address = AddressUK.builder().build();

            PCSCase pcsCase = PCSCase.builder()
                .propertyAddress(address)
                .manageHearingOption(ManageHearingOption.CANCEL)
                .showManageHearingPage(VerticalYesNo.YES)
                .caseNameHmctsInternal("Claimant v Defendant")
                .build();

            // When
            callSubmitHandler(pcsCase);

            // Then
            verify(hearingService, never()).addHearing(TEST_CASE_REFERENCE, pcsCase);
        }
    }

    private static PartyEntity createParty(UUID partyId) {
        PartyEntity partyEntity = mock(PartyEntity.class);
        when(partyEntity.getId()).thenReturn(partyId);
        return partyEntity;
    }

    private static ClaimPartyEntity createClaimParty(PartyRole role, PartyEntity partyEntity) {
        return ClaimPartyEntity.builder()
            .role(role)
            .party(partyEntity)
            .build();
    }
}
