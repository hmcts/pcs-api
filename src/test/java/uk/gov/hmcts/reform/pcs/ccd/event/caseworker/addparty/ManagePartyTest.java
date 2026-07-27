package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.addparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.ManagePartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class ManagePartyTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private ManagePartyService managePartyService;

    @BeforeEach
    void setUp() {
        ManageParty underTest = new ManageParty(pcsCaseService, partyService, addressFormatter, managePartyService);
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBuildPartyRadioList() {
        // Given
        PartyEntity claimantParty = PartyEntity.builder()
            .id(UUID.randomUUID()).nameKnown(VerticalYesNo.YES).build();
        PartyEntity defendantParty = PartyEntity.builder()
            .id(UUID.randomUUID()).nameKnown(VerticalYesNo.NO).build();
        PartyEntity litigationFriendParty = PartyEntity.builder().id(UUID.randomUUID()).build();

        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(claimantParty).role(PartyRole.CLAIMANT).build(),
                ClaimPartyEntity.builder().party(defendantParty).role(PartyRole.DEFENDANT).build(),
                ClaimPartyEntity.builder().party(litigationFriendParty).role(PartyRole.LITIGATION_FRIEND).build()
            ))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        when(partyService.getPartyName(claimantParty)).thenReturn("Jane Doe");
        when(partyService.getPartyLabel(mainClaim, claimantParty.getId())).thenReturn("Claimant 1");
        when(partyService.getPartyLabel(mainClaim, defendantParty.getId())).thenReturn("Defendant 1");

        PCSCase caseData = PCSCase.builder().addPartyDetails(AddPartyDetails.builder().build()).build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        List<DynamicListElement> listItems = result.getAddPartyDetails().getPartyRadioList().getListItems();
        assertThat(listItems).hasSize(2);
        assertThat(listItems.get(0).getLabel()).isEqualTo("Jane Doe - Claimant 1");
        assertThat(listItems.get(1).getLabel()).isEqualTo("Person unknown - Defendant 1");
        verify(partyService, never()).getPartyName(litigationFriendParty);
    }

    @ParameterizedTest
    @MethodSource("confirmationPageScenarios")
    void shouldBuildConfirmationPage(AddPartyDetails partyDetails, UUID actingForPartyId, String expectedPartyName) {
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

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        verify(managePartyService).addParty(partyDetails, pcsCaseEntity, mainClaim, actingForPartyId);

        assertThat(response.getConfirmationBody())
            .contains("Party " + expectedPartyName + " added")
            .contains("Case number: " + TEST_CASE_REFERENCE)
            .contains("1 Test Street, Test Town")
            .contains("Smith v Jones");
    }

    private static Stream<Arguments> confirmationPageScenarios() {
        UUID actingForPartyId = UUID.randomUUID();
        DynamicList partyRadioList = DynamicList.builder()
            .value(DynamicListElement.builder().code(actingForPartyId).label("Jane Doe - Claimant 1").build())
            .build();

        return Stream.of(
            Arguments.of(
                AddPartyDetails.builder()
                    .managePartyOptions(ManagePartyOptions.ADD_PARTY)
                    .addPartyType(PartyType.CLAIMANT)
                    .claimantName("Jane Doe")
                    .build(),
                null,
                "Jane Doe"
            ),
            Arguments.of(
                AddPartyDetails.builder()
                    .managePartyOptions(ManagePartyOptions.ADD_PARTY)
                    .addPartyType(PartyType.DEFENDANT)
                    .firstName("John")
                    .lastName("Smith")
                    .build(),
                null,
                "John Smith"
            ),
            Arguments.of(
                AddPartyDetails.builder()
                    .managePartyOptions(ManagePartyOptions.ADD_PARTY)
                    .addPartyType(PartyType.LITIGATION_FRIEND)
                    .litigationFriendOrganisationName("Acme Ltd")
                    .litigationFriendName("Bob Jones")
                    .partyRadioList(partyRadioList)
                    .build(),
                actingForPartyId,
                "Acme Ltd"
            )
        );
    }
}
