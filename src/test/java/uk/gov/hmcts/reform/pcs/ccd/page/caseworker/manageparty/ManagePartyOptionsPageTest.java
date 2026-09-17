package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.LAST_PARTY_ERROR;

@ExtendWith(MockitoExtension.class)
class ManagePartyOptionsPageTest extends BasePageTest {

    @Mock
    private PartyService partyService;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private RemovePartyService removePartyService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ManagePartyOptionsPage(
            partyService, addressMapper, addressFormatter, pcsCaseService, removePartyService));
    }

    @Test
    void shouldOnlyPrepopulateOnUpdate() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().managePartyOptions(ManagePartyOptions.ADD_PARTY).build())
            .updatePartyDetails(UpdatePartyDetails.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        verifyNoInteractions(partyService, addressMapper);
    }

    @Test
    void shouldPrepopulatePartyDetails() {
        // Given
        UUID partyId = UUID.randomUUID();
        UUID previousPartyId = UUID.randomUUID();

        AddressEntity addressEntity = AddressEntity.builder().build();
        AddressUK mappedAddress = AddressUK.builder()
            .addressLine1("1 Test Street")
            .postTown("Testville")
            .build();
        LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .address(addressEntity)
            .emailAddress("john@test.com")
            .phoneNumber("07000000000")
            .dateOfBirth(dateOfBirth)
            .build();

        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyService.getPartyRole(partyEntity)).thenReturn(PartyRole.DEFENDANT);
        when(addressMapper.toAddressUK(addressEntity)).thenReturn(mappedAddress);

        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyToUpdate(buildPartyRadioList(partyId))
            .previouslySelectedPartyId(previousPartyId.toString())
            .build();

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().managePartyOptions(ManagePartyOptions.UPDATE).build())
            .updatePartyDetails(updatePartyDetails)
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(updatePartyDetails.getPartyType()).isEqualTo(PartyType.DEFENDANT);
        assertThat(updatePartyDetails.getAddress().getAddressLine1()).isEqualTo("1 Test Street");
        assertThat(updatePartyDetails.getAddress().getPostTown()).isEqualTo("Testville");
        assertThat(updatePartyDetails.getEmail()).isEqualTo("john@test.com");
        assertThat(updatePartyDetails.getPhoneNumber()).isEqualTo("07000000000");
        assertThat(updatePartyDetails.getDateOfBirth()).contains(dateOfBirth);
        assertThat(updatePartyDetails.getPreviouslySelectedPartyId()).isEqualTo(partyId.toString());
    }

    @Test
    void shouldClearAddressFields() {
        // Given
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder().id(partyId).build();

        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyService.getPartyRole(partyEntity)).thenReturn(PartyRole.CLAIMANT);

        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyToUpdate(buildPartyRadioList(partyId))
            .build();

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().managePartyOptions(ManagePartyOptions.UPDATE).build())
            .updatePartyDetails(updatePartyDetails)
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(updatePartyDetails.getAddress()).isNotNull();
        assertThat(updatePartyDetails.getAddress().getAddressLine1()).isEmpty();
        assertThat(updatePartyDetails.getAddress().getPostTown()).isEmpty();
        assertThat(updatePartyDetails.getAddress().getCounty()).isEmpty();
    }

    @Test
    void shouldOnlyPrepopulateWhenNewPartySelected() {
        // Given
        UUID partyId = UUID.randomUUID();
        LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
        PartyEntity partyEntity = PartyEntity.builder().id(partyId).dateOfBirth(dateOfBirth).build();

        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);

        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyToUpdate(buildPartyRadioList(partyId))
            .previouslySelectedPartyId(partyId.toString())
            .email("caseworker-entered@test.com")
            .build();

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().managePartyOptions(ManagePartyOptions.UPDATE).build())
            .updatePartyDetails(updatePartyDetails)
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(updatePartyDetails.getEmail()).isEqualTo("caseworker-entered@test.com");
        verify(partyService, never()).getPartyRole(partyEntity);
        verifyNoInteractions(addressMapper);
    }

    @Test
    void shouldPrepopulateRemovePartyDetails() {
        // Given
        UUID partyId = UUID.randomUUID();
        AddressEntity addressEntity = AddressEntity.builder().addressLine1("1 Test Street").build();

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .address(addressEntity)
            .build();
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        partyEntity.setPcsCase(pcsCaseEntity);

        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyService.getPartyRole(partyEntity)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.getPartyName(partyEntity)).thenReturn("Billy Wright");
        when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn("Defendant 1");
        AddressUK mappedAddress = AddressUK.builder().addressLine1("1 Test Street").build();
        when(addressMapper.toAddressUK(addressEntity)).thenReturn(mappedAddress);
        when(addressFormatter.formatFullAddress(mappedAddress, "\n")).thenReturn("1 Test Street");

        RemovePartyDetails removePartyDetails = RemovePartyDetails.builder()
            .partyToRemove(buildPartyRadioList(partyId))
            .build();

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().managePartyOptions(ManagePartyOptions.REMOVE_PARTY).build())
            .removePartyDetails(removePartyDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
        assertThat(removePartyDetails.getSelectedPartyLabel()).isEqualTo("Billy Wright - Defendant 1");
        assertThat(removePartyDetails.getPartyType()).isEqualTo(PartyType.DEFENDANT);
        assertThat(removePartyDetails.getDateOfBirth()).isEqualTo("01/01/1990");
        assertThat(removePartyDetails.getAddress()).isEqualTo("1 Test Street");
    }

    @Test
    void shouldShowUnknownValuesWhenRemovePartyDetailsAreMissing() {
        // Given
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder().id(partyId).build();
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        partyEntity.setPcsCase(pcsCaseEntity);

        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyService.getPartyRole(partyEntity)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.getPartyName(partyEntity)).thenReturn("Person unknown");
        when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn("Defendant 1");

        RemovePartyDetails removePartyDetails = RemovePartyDetails.builder()
            .partyToRemove(buildPartyRadioList(partyId))
            .build();

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().managePartyOptions(ManagePartyOptions.REMOVE_PARTY).build())
            .removePartyDetails(removePartyDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
        assertThat(removePartyDetails.getSelectedPartyLabel()).isEqualTo("Person unknown - Defendant 1");
        assertThat(removePartyDetails.getDateOfBirth()).isEqualTo("Date of birth unknown");
        assertThat(removePartyDetails.getAddress()).isEqualTo("Address unknown");
    }

    @Test
    void shouldRejectRemoveWhenThereAreNoRemovableParties() {
        // Given
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimEntity mainClaim = ClaimEntity.builder().build();
        mainClaim.addParty(claimant, PartyRole.CLAIMANT);
        mainClaim.addParty(defendant, PartyRole.DEFENDANT);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().claims(List.of(mainClaim)).build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(removePartyService.hasAnyRemovableParty(mainClaim)).thenReturn(false);

        PCSCase caseData = PCSCase.builder()
            .addPartyDetails(AddPartyDetails.builder().managePartyOptions(ManagePartyOptions.REMOVE_PARTY).build())
            .removePartyDetails(RemovePartyDetails.builder().build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo(LAST_PARTY_ERROR);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    private DynamicList buildPartyRadioList(UUID partyId) {
        return DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).label("Jane Doe - Claimant 1").build())
            .build();
    }
}
