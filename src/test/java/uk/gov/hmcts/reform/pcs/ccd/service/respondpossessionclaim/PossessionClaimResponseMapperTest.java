package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PossessionClaimResponseMapperTest {

    @Mock
    private AddressMapper addressMapper;

    private PossessionClaimResponseMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new PossessionClaimResponseMapper(addressMapper);
    }

    @Test
    void shouldMapDefendantDataWithContactDetails() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postcode("SW1A 1AA")
            .build();

        PartyEntity matchedDefendant = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .nameKnown(VerticalYesNo.YES)
            .emailAddress("john@example.com")
            .phoneNumber("07700900000")
            .phoneNumberProvided(VerticalYesNo.YES)
            .address(addressEntity)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .build();

        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("Property Street")
            .postCode("M1 1AA")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postCode("SW1A 1AA")
            .build();

        when(addressMapper.toAddressUK(addressEntity)).thenReturn(expectedAddress);

        // When
        PossessionClaimResponse result = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDefendantContactDetails()).isNotNull();

        DefendantContactDetails contactDetails = result.getDefendantContactDetails();
        assertThat(contactDetails).isNotNull();
        assertThat(contactDetails.getParty().getFirstName()).isEqualTo("John");
        assertThat(contactDetails.getParty().getLastName()).isEqualTo("Doe");
        assertThat(contactDetails.getParty().getEmailAddress()).isEqualTo("john@example.com");
        assertThat(contactDetails.getParty().getPhoneNumber()).isEqualTo("07700900000");
        assertThat(contactDetails.getParty().getAddress()).isEqualTo(expectedAddress);
        assertThat(contactDetails.getParty().getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(contactDetails.getParty().getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(contactDetails.getParty().getPhoneNumberProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(contactDetails.getParty().getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldUsePropertyAddressWhenAddressSameAsPropertyIsYes() {
        // Given
        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("Jane")
            .lastName("Smith")
            .addressSameAsProperty(VerticalYesNo.YES)
            .address(null)
            .build();

        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("Property Street")
            .postCode("M1 1AA")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        // When
        PossessionClaimResponse result = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then
        assertThat(result.getDefendantContactDetails().getParty().getAddress())
            .isEqualTo(propertyAddress);
    }

    @Test
    void shouldUseDefendantAddressWhenAddressSameAsPropertyIsNo() {
        // Given
        AddressEntity defendantAddress = AddressEntity.builder()
            .addressLine1("Defendant Street")
            .postcode("B1 1AA")
            .build();

        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("Bob")
            .lastName("Jones")
            .addressSameAsProperty(VerticalYesNo.NO)
            .address(defendantAddress)
            .build();

        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("Property Street")
            .postCode("M1 1AA")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("Defendant Street")
            .postCode("B1 1AA")
            .build();

        when(addressMapper.toAddressUK(defendantAddress)).thenReturn(expectedAddress);

        // When
        PossessionClaimResponse result = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then
        verify(addressMapper).toAddressUK(defendantAddress);
        assertThat(result.getDefendantContactDetails().getParty().getAddress())
            .isEqualTo(expectedAddress);
    }

    @Test
    void shouldHandleNullAddressWhenAddressSameAsPropertyIsNull() {
        // Given
        PartyEntity matchedDefendant = PartyEntity.builder()
            .addressSameAsProperty(null)
            .address(null)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(AddressUK.builder().build())
            .build();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then
        assertThat(result.getDefendantContactDetails()).isNotNull();
        verify(addressMapper).toAddressUK(null);
    }

    @Test
    void shouldInitializeDefendantResponsesAsEmpty() {
        // Given
        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("Test")
            .lastName("User")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(AddressUK.builder().build())
            .build();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then
        assertThat(result.getDefendantResponses()).isNotNull();
    }

    @Test
    void shouldMapAllDefendantFields() {
        // Given
        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Full Street")
            .addressLine2("Apartment 4B")
            .postcode("SW1A 1AA")
            .build();

        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("John")
            .lastName("Doe")
            .nameKnown(VerticalYesNo.YES)
            .emailAddress("john.doe@example.com")
            .phoneNumber("07700900123")
            .phoneNumberProvided(VerticalYesNo.YES)
            .address(addressEntity)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(AddressUK.builder().build())
            .build();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Full Street")
            .addressLine2("Apartment 4B")
            .postCode("SW1A 1AA")
            .build();

        when(addressMapper.toAddressUK(addressEntity)).thenReturn(expectedAddress);

        // When
        PossessionClaimResponse result = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then
        uk.gov.hmcts.reform.pcs.ccd.domain.Party defendantParty = result.getDefendantContactDetails()
            .getParty();

        assertThat(defendantParty.getFirstName()).isEqualTo("John");
        assertThat(defendantParty.getLastName()).isEqualTo("Doe");
        assertThat(defendantParty.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantParty.getEmailAddress()).isEqualTo("john.doe@example.com");
        assertThat(defendantParty.getPhoneNumber()).isEqualTo("07700900123");
        assertThat(defendantParty.getPhoneNumberProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantParty.getAddress()).isEqualTo(expectedAddress);
        assertThat(defendantParty.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    @DisplayName("Should extract single claimant organisation from CLAIMANT-role parties")
    void shouldExtractSingleClaimantOrganisation() {
        // Given: PCSCase with one claimant (PartyRole.CLAIMANT, filtered by PCSCaseView)
        Party claimantParty = Party.builder()
            .orgName("ABC Housing Association")
            .build();

        List<ListValue<Party>> allClaimants = List.of(
            ListValue.<Party>builder()
                .id("claimant-1")
                .value(claimantParty)
                .build()
        );

        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(allClaimants)
            .propertyAddress(AddressUK.builder().build())
            .build();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When: Mapping to PossessionClaimResponse
        PossessionClaimResponse response = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then: Claimant organisation is extracted and wrapped in ListValue
        assertThat(response.getClaimantOrganisations())
            .hasSize(1);
        assertThat(response.getClaimantOrganisations().get(0).getId())
            .isEqualTo("claimant-org-1");
        assertThat(response.getClaimantOrganisations().get(0).getValue())
            .isEqualTo("ABC Housing Association");
    }

    @Test
    @DisplayName("Should extract multiple claimant organisations for joint landlord cases")
    void shouldExtractMultipleClaimantOrganisations() {
        // Given: Case with multiple CLAIMANT-role parties (joint landlords)
        List<ListValue<Party>> allClaimants = List.of(
            ListValue.<Party>builder()
                .id("claimant-1")
                .value(Party.builder().orgName("Primary Landlord Ltd").build())
                .build(),
            ListValue.<Party>builder()
                .id("claimant-2")
                .value(Party.builder().orgName("Secondary Housing Corp").build())
                .build(),
            ListValue.<Party>builder()
                .id("claimant-3")
                .value(Party.builder().orgName("Third Party Association").build())
                .build()
        );

        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(allClaimants)
            .propertyAddress(AddressUK.builder().build())
            .build();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse response = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then: All organisations extracted in order and wrapped in ListValue
        assertThat(response.getClaimantOrganisations()).hasSize(3);
        assertThat(response.getClaimantOrganisations().get(0).getId()).isEqualTo("claimant-org-1");
        assertThat(response.getClaimantOrganisations().get(0).getValue()).isEqualTo("Primary Landlord Ltd");
        assertThat(response.getClaimantOrganisations().get(1).getId()).isEqualTo("claimant-org-2");
        assertThat(response.getClaimantOrganisations().get(1).getValue()).isEqualTo("Secondary Housing Corp");
        assertThat(response.getClaimantOrganisations().get(2).getId()).isEqualTo("claimant-org-3");
        assertThat(response.getClaimantOrganisations().get(2).getValue()).isEqualTo("Third Party Association");
    }

    @Test
    @DisplayName("Should filter out null and empty organisation names")
    void shouldFilterOutNullAndEmptyOrganisations() {
        // Given: CLAIMANT parties with mixed valid, null, and empty orgNames
        List<ListValue<Party>> allClaimants = List.of(
            ListValue.<Party>builder()
                .id("claimant-1")
                .value(Party.builder().orgName("Valid Org").build())
                .build(),
            ListValue.<Party>builder()
                .id("claimant-2")
                .value(Party.builder().orgName(null).build())  // null orgName
                .build(),
            ListValue.<Party>builder()
                .id("claimant-3")
                .value(Party.builder().orgName("").build())  // empty string
                .build(),
            ListValue.<Party>builder()
                .id("claimant-4")
                .value(Party.builder().orgName("   ").build())  // whitespace only
                .build(),
            ListValue.<Party>builder()
                .id("claimant-5")
                .value(Party.builder().orgName("Another Valid Org").build())
                .build()
        );

        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(allClaimants)
            .propertyAddress(AddressUK.builder().build())
            .build();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse response = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then: Only valid non-empty orgs returned wrapped in ListValue
        assertThat(response.getClaimantOrganisations()).hasSize(2);
        assertThat(response.getClaimantOrganisations().get(0).getId()).isEqualTo("claimant-org-1");
        assertThat(response.getClaimantOrganisations().get(0).getValue()).isEqualTo("Valid Org");
        assertThat(response.getClaimantOrganisations().get(1).getId()).isEqualTo("claimant-org-2");
        assertThat(response.getClaimantOrganisations().get(1).getValue()).isEqualTo("Another Valid Org");
    }

    @Test
    @DisplayName("Should return empty list when allClaimants is null")
    void shouldReturnEmptyListWhenAllClaimantsIsNull() {
        // Given: PCSCase with null allClaimants (no CLAIMANT-role parties)
        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(null)
            .propertyAddress(AddressUK.builder().build())
            .build();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse response = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then: Empty list returned (not null)
        assertThat(response.getClaimantOrganisations())
            .isNotNull()
            .isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when allClaimants is empty")
    void shouldReturnEmptyListWhenAllClaimantsIsEmpty() {
        // Given: PCSCase with empty allClaimants list
        PartyEntity matchedDefendant = PartyEntity.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(List.of())
            .propertyAddress(AddressUK.builder().build())
            .build();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse response = underTest.mapFrom(pcsCase, matchedDefendant);

        // Then: Empty list returned
        assertThat(response.getClaimantOrganisations()).isEmpty();
    }
}
