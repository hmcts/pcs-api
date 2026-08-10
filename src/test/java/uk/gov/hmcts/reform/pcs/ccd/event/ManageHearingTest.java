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
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private PcsCaseEntity pcsCaseEntity;

    private ManageHearing manageHearing;
    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();

    @BeforeEach
    void setUp() {
        manageHearing = new ManageHearing(
            manageHearingConfigurer,
            addressFormatter,
            hearingService,
            locationReferenceService,
            pcsCaseService
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
            when(hearingService.buildPartyList(pcsCaseEntity)).thenReturn(DynamicMultiSelectStringList.builder()
                .value(List.of())
                .listItems(List.of())
                .build());
            when(caseLocation.getBaseLocation()).thenReturn(Integer.toString(BASE_LOCATION_ID));
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
            verify(hearingService, never()).setSelectedEditableHearingId(response, pcsCaseEntity);
            verify(hearingService, never()).clearHearingForm(response);
        }

        @Test
        void shouldRetainDraftHearingFormDataWhenHearingExists() {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearingList(List.of(hearingListValue(Hearing.builder().build())))
                .hearing(Hearing.builder()
                    .type(HearingType.POSSESSION)
                    .noticeWording(HearingNoticeWording.TPL)
                    .date(LocalDateTime.of(2026, 8, 5, 10, 30))
                    .durationDays(0)
                    .durationHours(1f)
                    .durationMinutes(30f)
                    .notes("stale notes")
                    .issueNotice(VerticalYesNo.YES)
                    .isWithoutNotice(VerticalYesNo.YES)
                    .additionalInformation("stale information")
                    .build())
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            verify(hearingService, never()).clearHearingForm(response);
            assertThat(response.getHearing().getNotes()).isEqualTo("stale notes");
            assertThat(response.getHearing().getAdditionalInformation()).isEqualTo("stale information");
        }

        @Test
        void shouldSerialiseRetainedHearingFormData() throws Exception {
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
            assertThat(serialisedResponse.get("hearing_Type").asText()).isEqualTo("POSSESSION");
            assertThat(serialisedResponse.get("hearing_NoticeWording").asText()).isEqualTo("TPL");
        }

        @Test
        void shouldNotPreselectNoticeRecipientPartiesUntilEditHearingIsSelected() {
            // Given
            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .hearingList(List.of(hearingListValue(Hearing.builder().build())))
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            verify(hearingService, never()).setSelectedEditableHearingId(response, pcsCaseEntity);
            assertThat(response.getPartyMultiSelectionList().getValue()).isEmpty();
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
            verify(hearingService).clearHearingForm(response);
        }

        @Test
        void shouldSetPartyList() {
            // Given
            DynamicMultiSelectStringList partyList = DynamicMultiSelectStringList.builder()
                .value(List.of())
                .listItems(List.of(DynamicStringListElement.builder()
                    .code(UUID.randomUUID().toString())
                    .label("claimant name - claimant label")
                    .build()))
                .build();
            when(hearingService.buildPartyList(pcsCaseEntity)).thenReturn(partyList);

            PCSCase pcsCase = PCSCase.builder()
                .caseManagementLocation(caseLocation)
                .build();

            // When
            PCSCase response = callStartHandler(pcsCase);

            // Then
            DynamicMultiSelectStringList partyMultiSelectionList = response.getPartyMultiSelectionList();
            assertThat(partyMultiSelectionList).usingRecursiveComparison().isEqualTo(partyList);
            verify(hearingService).buildPartyList(pcsCaseEntity);
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
                    <span class="govuk-panel__title govuk-!-font-size-32">Hearing added</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">Case number #%s</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">address</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">Claimant v Defendant</span><br>
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
                    <span class="govuk-panel__title govuk-!-font-size-32">Hearing added</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">Case number #1234</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">address</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">Claimant v Defendant</span><br>
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
                    <span class="govuk-panel__title govuk-!-font-size-32">Hearing edited</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">Case number #1234</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">address</span><br>
                    <span class="govuk-panel__body govuk-!-font-size-24">Claimant v Defendant</span><br>
                    </div>

                    <h3>What happens next</h3>

                    A hearing notice will be issued if you specified one is needed.
                    """
            );
        }
    }

    private static ListValue<Hearing> hearingListValue(Hearing hearing) {
        return ListValue.<Hearing>builder()
            .value(hearing)
            .build();
    }
}
