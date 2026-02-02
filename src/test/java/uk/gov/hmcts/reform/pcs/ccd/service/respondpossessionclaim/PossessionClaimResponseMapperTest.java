package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantData;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

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
        assertThat(result.getDefendantData()).isNotNull();

        DefendantData defendantData = result.getDefendantData();
        assertThat(defendantData.getContactDetails()).isNotNull();
        assertThat(defendantData.getContactDetails().getParty().getFirstName()).isEqualTo("John");
        assertThat(defendantData.getContactDetails().getParty().getLastName()).isEqualTo("Doe");
        assertThat(defendantData.getContactDetails().getParty().getEmailAddress()).isEqualTo("john@example.com");
        assertThat(defendantData.getContactDetails().getParty().getPhoneNumber()).isEqualTo("07700900000");
        assertThat(defendantData.getContactDetails().getParty().getAddress()).isEqualTo(expectedAddress);
        assertThat(defendantData.getContactDetails().getParty().getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantData.getContactDetails().getParty().getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantData.getContactDetails().getParty().getPhoneNumberProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantData.getContactDetails().getParty().getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);
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
        assertThat(result.getDefendantData().getContactDetails().getParty().getAddress())
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
        assertThat(result.getDefendantData().getContactDetails().getParty().getAddress())
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
        assertThat(result.getDefendantData()).isNotNull();
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
        assertThat(result.getDefendantData().getResponses()).isNotNull();
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
        uk.gov.hmcts.reform.pcs.ccd.domain.Party defendantParty = result.getDefendantData()
            .getContactDetails().getParty();

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
}
