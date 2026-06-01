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
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
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
class LegalRepresentativePageServiceTest {

    @InjectMocks
    private LegalRepresentativePageService legalRepresentativePageService;

    @Mock
    private LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private SecurityContextService securityContextService;

    @Test
    void save_WithDifferentPostalAddress() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        AddressUK address = AddressUK.builder().build();
        AddressEntity mappedAddress = new AddressEntity();

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.YES)
            .updatedCorrespondenceAddress(address)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());

        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        when(addressMapper.toAddressEntityAndNormalise(address))
            .thenReturn(mappedAddress);

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertEquals(mappedAddress, legalRepresentativeOrganisationEntity.getAddress());

        verify(addressMapper).toAddressEntityAndNormalise(address);
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntity);
    }

    @Test
    void save_WithDifferentPostalAddressAndNullCorrespondenceAddress() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.YES)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertThat(legalRepresentativeOrganisationEntity.getAddress()).isNull();

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntity);
    }

    @Test
    void save_WithSamePostalAddress() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntity);
    }

    @Test
    void save_WithDifferentPhoneNumber() {
        // given
        String contactNumber = "number";
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .provideContactPhoneNumber(VerticalYesNo.YES)
            .contactPhoneNumber(contactNumber)
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertEquals(contactNumber, legalRepresentativeOrganisationEntity.getPhone());
    }

    @Test
    void save_WithSamePhoneNumber_DoesNotSetPhone() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .provideContactPhoneNumber(VerticalYesNo.NO)
            .contactPhoneNumber("number")
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertNull(legalRepresentativeOrganisationEntity.getPhone());
    }

    @Test
    void save_WithNullProvideContactPhoneNumber_DoesNotSetPhone() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .contactPhoneNumber("number")
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertNull(legalRepresentativeOrganisationEntity.getPhone());
    }

    @Test
    void save_WithReference() {
        // given
        String reference = "reference";
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .reference(reference)
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertEquals(reference, legalRepresentativeOrganisationEntity.getContactReference());
    }

    @Test
    void save_WithEmptyReference_DoesNotSetReference() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .reference("")
            .differentPostalAddress(VerticalYesNo.NO)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertNull(legalRepresentativeOrganisationEntity.getContactReference());
    }

    @Test
    void save_WithUseEmailNo_SetsEmail() {
        // given
        String orgId = "org";
        long caseReference = 1L;
        String email = "email";

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .useEmailAddress(VerticalYesNo.NO)
            .emailAddress(email)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertEquals(email, legalRepresentativeOrganisationEntity.getEmail());
    }

    @Test
    void save_WithUseEmailYes_DoesNotSetEmail() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .useEmailAddress(VerticalYesNo.YES)
            .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            new LegalRepresentativeOrganisationEntity();

        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        assertThat(legalRepresentativeOrganisationEntity.getEmail()).isNull();
    }

    @Test
    void save_WithExistingLegalRep_DoesNotCreate() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .useEmailAddress(VerticalYesNo.YES)
            .build();

        LegalRepresentativeEntity legalRepresentative = LegalRepresentativeEntity.builder().build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            LegalRepresentativeOrganisationEntity.builder().legalRepresentativeList(List.of(legalRepresentative))
                .build();

        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationEntity));

        // when
        legalRepresentativePageService.save(orgId, caseReference, legalRepresentativeDetails);

        // then
        verify(securityContextService, never()).getCurrentUserId();
    }

}
