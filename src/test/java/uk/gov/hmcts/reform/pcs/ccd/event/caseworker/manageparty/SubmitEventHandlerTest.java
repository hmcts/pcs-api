package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.AddPartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.UpdatePartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private AddPartyService addPartyService;
    @Mock
    private UpdatePartyService updatePartyService;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(pcsCaseService, addressFormatter, addPartyService, updatePartyService);
    }

    @ParameterizedTest
    @MethodSource("addConfirmationPageScenarios")
    void shouldBuildAddConfirmationPage(AddPartyDetails partyDetails, UUID actingForPartyId,
                                         String expectedPartyDescription) {
        // Given
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 Test Street").build();
        when(addressFormatter.formatShortAddress(propertyAddress, COMMA_DELIMITER))
            .thenReturn("1 Test Street, Test Town");

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(partyDetails)
            .propertyAddress(propertyAddress)
            .caseNameHmctsInternal("Smith v Jones")
            .build();
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);

        // When
        SubmitResponse<State> response = underTest.submit(eventPayload);

        // Then
        verify(addPartyService).addParty(partyDetails, pcsCaseEntity, mainClaim, actingForPartyId);

        assertThat(response.getConfirmationBody())
            .contains(expectedPartyDescription + " added")
            .contains("Case number: " + TEST_CASE_REFERENCE)
            .contains("1 Test Street, Test Town")
            .contains("Smith v Jones");
    }

    @ParameterizedTest
    @MethodSource("updateConfirmationPageScenarios")
    void shouldBuildUpdateConfirmationPage(PartyType partyType, String expectedPartyDescription) {
        // Given
        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyType(partyType)
            .build();

        AddPartyDetails partyDetails = AddPartyDetails.builder()
            .managePartyOptions(ManagePartyOptions.UPDATE)
            .build();

        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 Test Street").build();
        when(addressFormatter.formatShortAddress(propertyAddress, COMMA_DELIMITER))
            .thenReturn("1 Test Street, Test Town");

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(partyDetails)
            .updatePartyDetails(updatePartyDetails)
            .propertyAddress(propertyAddress)
            .caseNameHmctsInternal("Smith v Jones")
            .build();
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);

        // When
        SubmitResponse<State> response = underTest.submit(eventPayload);

        // Then
        verify(updatePartyService).updateParty(updatePartyDetails, TEST_CASE_REFERENCE);

        assertThat(response.getConfirmationBody())
            .contains(expectedPartyDescription + " details updated")
            .contains("Case number: " + TEST_CASE_REFERENCE)
            .contains("1 Test Street, Test Town")
            .contains("Smith v Jones");
    }

    private static Stream<Arguments> updateConfirmationPageScenarios() {
        return Stream.of(
            Arguments.of(PartyType.CLAIMANT, "Claimant's"),
            Arguments.of(PartyType.DEFENDANT, "Defendant's")
        );
    }

    private static Stream<Arguments> addConfirmationPageScenarios() {
        UUID actingForPartyId = UUID.randomUUID();
        DynamicList partyRadioList = DynamicList.builder()
            .value(DynamicListElement.builder().code(actingForPartyId).label("Jane Doe - Claimant 1").build())
            .build();

        return Stream.of(
            Arguments.of(
                AddPartyDetails.builder()
                    .managePartyOptions(ManagePartyOptions.ADD_PARTY)
                    .addPartyType(PartyType.CLAIMANT)
                    .claimantFirstName("Jane")
                    .claimantLastName("Doe")
                    .build(),
                null,
                "Claimant Jane Doe"
            ),
            Arguments.of(
                AddPartyDetails.builder()
                    .managePartyOptions(ManagePartyOptions.ADD_PARTY)
                    .addPartyType(PartyType.DEFENDANT)
                    .firstName("John")
                    .lastName("Smith")
                    .build(),
                null,
                "Defendant John Smith"
            ),
            Arguments.of(
                AddPartyDetails.builder()
                    .managePartyOptions(ManagePartyOptions.ADD_PARTY)
                    .addPartyType(PartyType.LITIGATION_FRIEND)
                    .litigationFriendOrganisationName("Acme Ltd")
                    .litigationFriendFirstName("Bob")
                    .litigationFriendLastName("Jones")
                    .partyRadioList(partyRadioList)
                    .build(),
                actingForPartyId,
                "Litigation friend Bob Jones"
            )
        );
    }

}