package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeServiceTest {

    @InjectMocks
    private LegalRepresentativeService legalRepresentativeService;

    @Mock
    private LegalRepresentativeRepository legalRepresentativeRepository;

    @Mock
    private AddressMapper addressMapper;

    @Test
    void save_WithDifferentPostalAddress() {
        // given
        UUID userIdamId = UUID.randomUUID();

        AddressUK address = AddressUK.builder().build();
        AddressEntity mappedAddress = new AddressEntity();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.YES)
            .updatedCorrespondenceAddress(address)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        when(addressMapper.toAddressEntityAndNormalise(address))
            .thenReturn(mappedAddress);

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertEquals(mappedAddress, legalRepresentativeEntity.getAddress());

        verify(addressMapper).toAddressEntityAndNormalise(address);
        verify(legalRepresentativeRepository).save(legalRepresentativeEntity);
    }

    @Test
    void save_WithDifferentPostalAddressAndNullCorrespondenceAddress() {
        // given
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.YES)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertThat(legalRepresentativeEntity.getAddress()).isNull();

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeRepository).save(legalRepresentativeEntity);
    }

    @Test
    void save_WithSamePostalAddress() {
        // given
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeRepository).save(legalRepresentativeEntity);
    }

    @Test
    void save_WithDifferentPhoneNumber() {
        // given
        String contactNumber = "number";
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .provideContactPhoneNumber(VerticalYesNo.YES)
            .contactPhoneNumber(contactNumber)
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertEquals(contactNumber, legalRepresentativeEntity.getPhone());
    }

    @Test
    void save_WithSamePhoneNumber_DoesNotSetPhone() {
        // given
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .provideContactPhoneNumber(VerticalYesNo.NO)
            .contactPhoneNumber("number")
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertNull(legalRepresentativeEntity.getPhone());
    }

    @Test
    void save_WithNullProvideContactPhoneNumber_DoesNotSetPhone() {
        // given
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .contactPhoneNumber("number")
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertNull(legalRepresentativeEntity.getPhone());
    }

    @Test
    void save_WithReference() {
        // given
        String reference = "reference";
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .reference(reference)
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertEquals(reference, legalRepresentativeEntity.getReference());
    }

    @Test
    void save_WithEmptyReference_DoesNotSetReference() {
        // given
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .reference("")
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertNull(legalRepresentativeEntity.getReference());
    }

    @Test
    void save_WithUseEmailNo_SetsEmail() {
        // given
        UUID userIdamId = UUID.randomUUID();
        String email = "email";

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .useEmailAddress(VerticalYesNo.NO)
            .emailAddress(email)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertEquals(email, legalRepresentativeEntity.getEmail());
    }

    @Test
    void save_WithUseEmailYes_DoesNotSetEmail() {
        // given
        UUID userIdamId = UUID.randomUUID();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .useEmailAddress(VerticalYesNo.YES)
            .build();

        LegalRepresentativeEntity legalRepresentativeEntity = new LegalRepresentativeEntity();

        when(legalRepresentativeRepository.findByIdamId(userIdamId))
            .thenReturn(Optional.of(legalRepresentativeEntity));

        // when
        legalRepresentativeService.save(userIdamId, legalRepresentativeDetails);

        // then
        assertThat(legalRepresentativeEntity.getEmail()).isNull();
    }

}
