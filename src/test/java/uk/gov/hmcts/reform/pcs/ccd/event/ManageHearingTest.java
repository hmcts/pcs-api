package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.CaseLocation;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ManageHearingTest extends BaseEventTest {

    @Mock
    private ManageHearingConfigurer manageHearingConfigurer;

    @Mock
    private AddressFormatter addressFormatter;

    @Mock
    private HearingService hearingService;

    @Mock
    private LocationReferenceService locationReferenceService;

    @InjectMocks
    private ManageHearing manageHearing;

    @BeforeEach
    void setUp() {
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
    void shouldShowManageHearingPageIfHearingExists() {
        // Given
        Hearing hearing = Hearing.builder().build();
        List<ListValue<Hearing>> hearingList = List.of(
            ListValue.<Hearing>builder()
                .value(hearing)
                .build()
        );

        Party claimant = Party.builder()
            .orgName("Claimant Name")
            .build();
        List<ListValue<Party>> allClaimants = List.of(
            ListValue.<Party>builder()
                .id("claimantId")
                .value(claimant)
                .build()
        );

        Party defendant = Party.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("Defendant")
            .lastName("One")
            .build();
        List<ListValue<Party>> allDefendants = List.of(
            ListValue.<Party>builder()
                .id("defendant1Id")
                .value(defendant)
                .build()
        );

        CaseLocation caseLocation = CaseLocation.builder()
            .baseLocation("1")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .caseManagementLocation(caseLocation)
            .hearingList(hearingList)
            .allClaimants(allClaimants)
            .allDefendants(allDefendants)
            .build();

        CourtVenue courtVenue = newCourtVenue("1", "Court name", "1");
        when(locationReferenceService.getCourtVenues(List.of(1))).thenReturn(List.of(courtVenue));

        // When
        PCSCase response = callStartHandler(pcsCase);

        // Then
        assertThat(response.getShowManageHearingPage()).isEqualTo(VerticalYesNo.YES);
        assertThat(response.getManageHearingOption()).isNull();
        assertThat(response.getHearingLocation()).isEqualTo("Court name");
        DynamicMultiSelectStringList partyMultiSelectionList = response.getPartyMultiSelectionList();
        assertThat(partyMultiSelectionList).isNotNull();
        List<DynamicStringListElement> listItems = partyMultiSelectionList.getListItems();
        assertThat(listItems).hasSize(2);
        assertThat(listItems.getFirst().getLabel()).isEqualTo("Claimant Name - Claimant 1");
        assertThat(listItems.getFirst().getCode()).isEqualTo("claimantId");
        assertThat(listItems.getLast().getLabel()).isEqualTo("Defendant One - Defendant 1");
        assertThat(listItems.getLast().getCode()).isEqualTo("defendant1Id");
    }

    @Test
    void shouldNotShowManageHearingPageIfHearingDoesNotExists() {
        // Given
        Party claimant = Party.builder()
            .orgName("Claimant Name")
            .build();
        List<ListValue<Party>> allClaimants = List.of(
            ListValue.<Party>builder()
                .id("claimantId")
                .value(claimant)
                .build()
        );

        Party defendant = Party.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("Defendant")
            .lastName("One")
            .build();
        List<ListValue<Party>> allDefendants = List.of(
            ListValue.<Party>builder()
                .id("defendant1Id")
                .value(defendant)
                .build()
        );

        CaseLocation caseLocation = CaseLocation.builder()
            .baseLocation("1")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .caseManagementLocation(caseLocation)
            .allClaimants(allClaimants)
            .allDefendants(allDefendants)
            .build();

        // When
        PCSCase response = callStartHandler(pcsCase);

        // Then
        assertThat(response.getShowManageHearingPage()).isNull();
        assertThat(response.getManageHearingOption()).isEqualTo(ManageHearingOption.ADD);
        DynamicMultiSelectStringList partyMultiSelectionList = response.getPartyMultiSelectionList();
        assertThat(partyMultiSelectionList).isNotNull();
        List<DynamicStringListElement> listItems = partyMultiSelectionList.getListItems();
        assertThat(listItems).hasSize(2);
        assertThat(listItems.getFirst().getLabel()).isEqualTo("Claimant Name - Claimant 1");
        assertThat(listItems.getFirst().getCode()).isEqualTo("claimantId");
        assertThat(listItems.getLast().getLabel()).isEqualTo("Defendant One - Defendant 1");
        assertThat(listItems.getLast().getCode()).isEqualTo("defendant1Id");
    }

    @Test
    void shouldHandleHearingVenueNotFound() {
        // Given
        Party claimant = Party.builder()
            .orgName("Claimant Name")
            .build();
        List<ListValue<Party>> allClaimants = List.of(
            ListValue.<Party>builder()
                .id("claimantId")
                .value(claimant)
                .build()
        );

        Party defendant = Party.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("Defendant")
            .lastName("One")
            .build();
        List<ListValue<Party>> allDefendants = List.of(
            ListValue.<Party>builder()
                .id("defendant1Id")
                .value(defendant)
                .build()
        );

        CaseLocation caseLocation = CaseLocation.builder()
            .baseLocation("1")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .caseManagementLocation(caseLocation)
            .allClaimants(allClaimants)
            .allDefendants(allDefendants)
            .build();

        when(locationReferenceService.getCourtVenues(List.of(1))).thenThrow(new RuntimeException());

        // When
        PCSCase response = callStartHandler(pcsCase);

        // Then
        assertThat(response.getHearingLocation()).isEqualTo("Unable to retrieve hearing location");
    }

    @Test
    void shouldShowDefendantNameAsPersonUnknownIfNameNotKnown() {
        // Given
        Party claimant = Party.builder()
            .orgName("Claimant Name")
            .build();
        List<ListValue<Party>> allClaimants = List.of(
            ListValue.<Party>builder()
                .id("claimantId")
                .value(claimant)
                .build()
        );

        Party defendant = Party.builder()
            .nameKnown(VerticalYesNo.NO)
            .firstName("Defendant")
            .lastName("One")
            .build();
        List<ListValue<Party>> allDefendants = List.of(
            ListValue.<Party>builder()
                .id("defendant1Id")
                .value(defendant)
                .build()
        );

        CaseLocation caseLocation = CaseLocation.builder()
            .baseLocation("1")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .caseManagementLocation(caseLocation)
            .allClaimants(allClaimants)
            .allDefendants(allDefendants)
            .build();

        // When
        PCSCase response = callStartHandler(pcsCase);

        // Then
        assertThat(response.getShowManageHearingPage()).isNull();
        assertThat(response.getManageHearingOption()).isEqualTo(ManageHearingOption.ADD);
        DynamicMultiSelectStringList partyMultiSelectionList = response.getPartyMultiSelectionList();
        assertThat(partyMultiSelectionList).isNotNull();
        List<DynamicStringListElement> listItems = partyMultiSelectionList.getListItems();
        assertThat(listItems).hasSize(2);
        assertThat(listItems.getFirst().getLabel()).isEqualTo("Claimant Name - Claimant 1");
        assertThat(listItems.getFirst().getCode()).isEqualTo("claimantId");
        assertThat(listItems.getLast().getLabel()).isEqualTo("Person unknown - Defendant 1");
        assertThat(listItems.getLast().getCode()).isEqualTo("defendant1Id");
    }

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
        verify(hearingService).addHearing(1234L, pcsCase);
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

            Work allocation tasks will be created for court staff to complete on the review dates.
            """
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
        verify(hearingService).addHearing(1234L, pcsCase);
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

            Work allocation tasks will be created for court staff to complete on the review dates.
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
        verify(hearingService, never()).addHearing(1234L, pcsCase);
    }

    CourtVenue newCourtVenue(String courtVenueId, String siteName, String epimmsId) {
        return new CourtVenue(
            courtVenueId,
            epimmsId,
            siteName,
            "1",
            "London",
            "Central London County Court",
            "7",
            null,
            null,
            "YES",
            "Brentford County Court, Alexandra Road",
            "TW8 0JJ",
            "020 1234 5678",
            null,
            "",
            "DX 12345 Brentford",
            "",
            "",
            "Open",
            null,
            siteName,
            siteName,
            "N",
            "Y",
            "",
            "N",
            "N",
            "COURT",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        );
    }
}
