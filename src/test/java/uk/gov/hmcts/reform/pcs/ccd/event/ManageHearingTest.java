package uk.gov.hmcts.reform.pcs.ccd.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.CaseLocation;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_ADMIN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.HEARING_CENTRE_TEAM_LEADER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

@ExtendWith(MockitoExtension.class)
public class ManageHearingTest extends BaseEventTest {

    private static final int BASE_LOCATION_ID = 1;

    @Mock
    private ManageHearingConfigurer manageHearingConfigurer;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private HearingService hearingService;
    @Mock
    private LocationReferenceService locationReferenceService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaim;

    private ManageHearing manageHearing;
    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();

    @BeforeEach
    void setUp() {
        manageHearing = new ManageHearing(
            manageHearingConfigurer,
            addressFormatter,
            hearingService,
            locationReferenceService,
            pcsCaseService,
            partyService
        );
        setEventUnderTest(manageHearing);
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

    @Test
    void shouldConfigureEventForConfirmedStates() {
        assertThat(configuredEvent.getPreState())
            .containsExactlyInAnyOrder(AWAITING_SUBMISSION_TO_HMCTS, PENDING_CASE_ISSUED, CASE_ISSUED);
    }

    @Test
    void shouldOnlyShowEventWhenReleaseAndCaseworkerEventsFeatureFlagsAreEnabled() {
        assertThat(configuredEvent.getShowCondition())
            .isEqualTo(featureFlagsEnabled(RELEASE_1_DOT_2, CASEWORKER_EVENTS));
    }

    @Test
    void shouldGrantAccessToHearingCentreRoles() {
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_ADMIN)).containsAll(Permission.CRUD);
        assertThat(configuredEvent.getGrants().get(HEARING_CENTRE_TEAM_LEADER)).containsAll(Permission.CRUD);
    }

    @Test
    void shouldGrantHistoryVisibilityToJudicialHistoryRoles() {
        assertThat(JUDICIAL_HISTORY_ROLES)
            .allSatisfy(role -> assertThat(configuredEvent.getGrants().get(role)).contains(Permission.R));
    }

    @Nested
    @DisplayName("Start Handler")
    class StartHandlerTests {

        @Mock
        private CaseLocation caseLocation;

        @BeforeEach
        void setUp() {
            when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
            when(pcsCaseEntity.getMainClaim()).thenReturn(mainClaim);
            when(mainClaim.getClaimParties()).thenReturn(List.of());
            when(caseLocation.getBaseLocation()).thenReturn(Integer.toString(BASE_LOCATION_ID));
            doAnswer(invocation -> {
                PCSCase caseData = invocation.getArgument(0);
                caseData.setHearing(Hearing.builder().build());

                DynamicMultiSelectList partyList = caseData.getPartyMultiSelectionList();
                if (partyList != null) {
                    caseData.setPartyMultiSelectionList(DynamicMultiSelectList.builder()
                        .listItems(partyList.getListItems())
                        .build());
                }

                return null;
            }).when(hearingService).clearHearingForm(any(PCSCase.class));
        }

        @Test
        void shouldShowManageHearingPageIfHearingExists() {
            // Given
            Hearing hearing = Hearing.builder().build();
            List<ListValue<Hearing>> hearingList = List.of(
                ListValue.<Hearing>builder().value(hearing).build()
            );

            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearingList(hearingList)
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getShowManageHearingPage()).isEqualTo(VerticalYesNo.YES);
            assertThat(response.getManageHearingOption()).isNull();
            verify(hearingService).clearHearingForm(response);
            verify(hearingService).setSelectedEditableHearingId(response, pcsCaseEntity);
        }

        @Test
        void shouldClearStaleHearingFormDataWhenHearingExists() {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearingList(List.of(hearingListValue(Hearing.builder().build())))
                .hearing(Hearing.builder()
                    .type(HearingType.POSSESSION)
                    .noticeWording(HearingNoticeWording.TPL)
                    .date(LocalDateTime.of(2026, 8, 5, 10, 30))
                    .durationDays(0)
                    .durationHours(1)
                    .durationMinutes(30)
                    .notes("stale notes")
                    .issueNotice(VerticalYesNo.YES)
                    .isWithoutNotice(VerticalYesNo.YES)
                    .additionalInformation("stale information")
                    .build())
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            verify(hearingService).clearHearingForm(response);
            assertThat(response.getHearing()).usingRecursiveComparison()
                .isEqualTo(Hearing.builder().build());
        }

        @Test
        void shouldSerialiseClearedHearingFormDataAsNullFields() throws Exception {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearingList(List.of(hearingListValue(Hearing.builder().build())))
                .hearing(Hearing.builder()
                    .type(HearingType.POSSESSION)
                    .noticeWording(HearingNoticeWording.TPL)
                    .build())
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);
            JsonNode serialisedResponse = objectMapper.valueToTree(response);

            // Then
            assertThat(serialisedResponse.has("hearing_Type")).isTrue();
            assertThat(serialisedResponse.get("hearing_Type").isNull()).isTrue();
            assertThat(serialisedResponse.has("hearing_NoticeWording")).isTrue();
            assertThat(serialisedResponse.get("hearing_NoticeWording").isNull()).isTrue();
        }

        @Test
        void shouldNotPreselectNoticeRecipientPartiesUntilEditHearingIsSelected() {
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
                .hearingList(List.of(hearingListValue(Hearing.builder().build())))
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            verify(hearingService).setSelectedEditableHearingId(response, pcsCaseEntity);
            assertThat(response.getPartyMultiSelectionList().getValue()).isNull();
        }

        @Test
        void shouldPreselectAddActionAndNotShowManageHearingPageIfHearingDoesNotExists() {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearingList(List.of())
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getShowManageHearingPage()).isEqualTo(VerticalYesNo.NO);
            assertThat(response.getManageHearingOption()).isEqualTo(ManageHearingOption.ADD);
            assertThat(response.getSelectedHearingId()).isNull();
            assertThat(response.getHearing()).usingRecursiveComparison()
                .isEqualTo(Hearing.builder().build());
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

            CourtVenue courtVenue = mock(CourtVenue.class);
            when(locationReferenceService.getCourtVenues(List.of(BASE_LOCATION_ID))).thenReturn(List.of(courtVenue));
            when(courtVenue.courtName()).thenReturn(expectedHearingLocation);

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            assertThat(response.getHearingLocation()).isEqualTo(expectedHearingLocation);
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

            when(addressFormatter.formatMediumAddress(address, AddressFormatter.COMMA_DELIMITER))
                .thenReturn("address");

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

            // Then
            verify(hearingService).addHearing(TEST_CASE_REFERENCE, pcsCase);
            verify(hearingService, never()).updateHearing(TEST_CASE_REFERENCE, pcsCase);
            assertThat(submitResponse.getConfirmationBody()).isEqualTo(
                """
                    ---
                    <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                    <span class="govuk-panel__title govuk-!-font-size-36">Hearing Added</span><br>
                    <span class="govuk-panel__body">Case number #%s</span><br>
                    <span class="govuk-panel__body">address</span><br>
                    <span class="govuk-panel__body">Claimant v Defendant</span><br>
                    </div>

                    <h3>What happens next</h3>

                    A hearing notice will be issued if you specified one is needed.
                    """.formatted(TEST_CASE_REFERENCE)
            );
        }

        @Test
        void shouldAddHearingOnSubmitWhenMangeHearingPageWasNotShown() {
            // Given
            AddressUK address = AddressUK.builder().build();

            PCSCase pcsCase = PCSCase.builder()
                .propertyAddress(address)
                .caseNameHmctsInternal("Claimant v Defendant")
                .build();

            when(addressFormatter.formatMediumAddress(address, AddressFormatter.COMMA_DELIMITER))
                .thenReturn("address");

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

            // Then
            verify(hearingService).addHearing(TEST_CASE_REFERENCE, pcsCase);
            verify(hearingService, never()).updateHearing(TEST_CASE_REFERENCE, pcsCase);
            assertThat(submitResponse.getConfirmationBody()).isEqualTo(
                """
                    ---
                    <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                    <span class="govuk-panel__title govuk-!-font-size-36">Hearing Added</span><br>
                    <span class="govuk-panel__body">Case number #1234</span><br>
                    <span class="govuk-panel__body">address</span><br>
                    <span class="govuk-panel__body">Claimant v Defendant</span><br>
                    </div>

                    <h3>What happens next</h3>

                    A hearing notice will be issued if you specified one is needed.
                    """
            );
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
            verify(hearingService, never()).updateHearing(TEST_CASE_REFERENCE, pcsCase);
        }

        @Test
        void shouldUpdateHearingOnSubmitWhenManageHearingOptionIsEdit() {
            // Given
            AddressUK address = AddressUK.builder().build();

            PCSCase pcsCase = PCSCase.builder()
                .propertyAddress(address)
                .manageHearingOption(ManageHearingOption.EDIT)
                .showManageHearingPage(VerticalYesNo.YES)
                .caseNameHmctsInternal("Claimant v Defendant")
                .build();

            when(addressFormatter.formatMediumAddress(address, AddressFormatter.COMMA_DELIMITER))
                .thenReturn("address");

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

            // Then
            verify(hearingService, never()).addHearing(TEST_CASE_REFERENCE, pcsCase);
            verify(hearingService).updateHearing(TEST_CASE_REFERENCE, pcsCase);
            assertThat(submitResponse.getConfirmationBody()).isEqualTo(
                """
                    ---
                    <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                    <span class="govuk-panel__title govuk-!-font-size-36">Hearing edited</span><br>
                    <span class="govuk-panel__body">Case number #1234</span><br>
                    <span class="govuk-panel__body">address</span><br>
                    </div>

                    <h3>What happens next</h3>

                    A hearing notice will be issued if you specified one is needed.
                    """
            );
            assertThat(submitResponse.getConfirmationBody()).doesNotContain("Claimant v Defendant");
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

    private static ListValue<Hearing> hearingListValue(Hearing hearing) {
        return ListValue.<Hearing>builder()
            .value(hearing)
            .build();
    }
}
