package uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePartyServiceTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PartyService partyService;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private AddressMapper addressMapper;
    @Captor
    private ArgumentCaptor<PartyEntity> partyEntityCaptor;

    private UpdatePartyService underTest;

    @BeforeEach
    void setUp() {
        underTest = new UpdatePartyService(partyService, partyRepository, addressMapper);
    }

    @Test
    void shouldUpdateParty() {
        // Given
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = new PartyEntity();
        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);

        AddressUK address = AddressUK.builder().addressLine1("1 Test Street").postTown("Testville").build();
        AddressEntity mappedAddress = new AddressEntity();
        when(addressMapper.toAddressEntityAndNormalise(address)).thenReturn(mappedAddress);

        LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyToUpdate(buildPartyRadioList(partyId))
            .partyType(PartyType.DEFENDANT)
            .address(address)
            .email("john@test.com")
            .phoneNumber("07000000000")
            .dateOfBirth(Optional.of(dateOfBirth))
            .build();

        // When
        underTest.updateParty(updatePartyDetails, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyEntityCaptor.capture());
        PartyEntity savedParty = partyEntityCaptor.getValue();
        assertThat(savedParty.getAddress()).isEqualTo(mappedAddress);
        assertThat(savedParty.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedParty.getEmailAddress()).isEqualTo("john@test.com");
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07000000000");
        assertThat(savedParty.getPhoneNumberProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedParty.getDateOfBirth()).isEqualTo(dateOfBirth);
    }

    @Test
    void shouldNotMarkPhoneNumberProvidedWhenBlank() {
        // Given
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = new PartyEntity();
        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);

        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyToUpdate(buildPartyRadioList(partyId))
            .partyType(PartyType.CLAIMANT)
            .email(" ")
            .phoneNumber(" ")
            .build();

        // When
        underTest.updateParty(updatePartyDetails, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyEntityCaptor.capture());
        PartyEntity savedParty = partyEntityCaptor.getValue();
        assertThat(savedParty.getEmailAddress()).isEqualTo(" ");
        assertThat(savedParty.getPhoneNumber()).isEqualTo(" ");
        assertThat(savedParty.getPhoneNumberProvided()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldNotUpdateDateOfBirthForClaimant() {
        // Given
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = new PartyEntity();
        when(partyService.getPartyEntityById(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);

        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyToUpdate(buildPartyRadioList(partyId))
            .partyType(PartyType.CLAIMANT)
            .dateOfBirth(Optional.of(LocalDate.of(1990, 1, 1)))
            .build();

        // When
        underTest.updateParty(updatePartyDetails, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyEntityCaptor.capture());
        assertThat(partyEntityCaptor.getValue().getDateOfBirth()).isNull();
    }

    private DynamicList buildPartyRadioList(UUID partyId) {
        return DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).label("Jane Doe - Claimant 1").build())
            .build();
    }
}
