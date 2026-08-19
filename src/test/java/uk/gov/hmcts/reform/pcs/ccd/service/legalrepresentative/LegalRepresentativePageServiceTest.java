package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativePageServiceTest {

    @InjectMocks
    private LegalRepresentativePageService legalRepresentativePageService;

    @Mock
    private ClaimPartyContactDetailsRepository
        claimPartyContactDetailsRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private AddressFormatter addressFormatter;

    @Test
    void save_WithDifferentPostalAddress() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        AddressUK address = AddressUK.builder().build();
        AddressEntity mappedAddress = new AddressEntity();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();
        final LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.YES)
            .updatedCorrespondenceAddress(address)
            .organisationId(orgId)
            .build();
        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));
        when(addressMapper.toAddressEntityAndNormalise(address))
            .thenReturn(mappedAddress);

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(mappedAddress, legalRepresentativeOrganisationContactDetails.getAddress());
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());

        verify(addressMapper).toAddressEntityAndNormalise(address);
        verify(claimPartyContactDetailsRepository)
            .save(legalRepresentativeOrganisationContactDetails);
    }

    @Test
    void save_WithDifferentPostalAddressAndNullCorrespondenceAddress() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.YES)
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();
        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertThat(legalRepresentativeOrganisationContactDetails.getAddress()).isNull();
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(claimPartyContactDetailsRepository)
            .save(legalRepresentativeOrganisationContactDetails);
    }

    @Test
    void save_WithSamePostalAddress() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.NO)
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(claimPartyContactDetailsRepository)
            .save(legalRepresentativeOrganisationContactDetails);
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
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        assertEquals(contactNumber, legalRepresentativeOrganisationContactDetails.getPhoneNumber());
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
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();
        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        assertNull(legalRepresentativeOrganisationContactDetails.getPhoneNumber());
    }

    @Test
    void save_WithNullProvideContactPhoneNumber_DoesNotSetPhone() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .contactPhoneNumber("number")
            .differentPostalAddress(VerticalYesNo.NO)
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        assertNull(legalRepresentativeOrganisationContactDetails.getPhoneNumber());
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
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        assertEquals(reference, legalRepresentativeOrganisationContactDetails.getContactReference());
    }

    @Test
    void save_WithEmptyReference_DoesNotSetReference() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .reference("")
            .differentPostalAddress(VerticalYesNo.NO)
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        assertNull(legalRepresentativeOrganisationContactDetails.getContactReference());
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
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        assertEquals(email, legalRepresentativeOrganisationContactDetails.getEmailAddress());
    }

    @Test
    void save_WithUseEmailYes_DoesNotSetEmail() {
        // given
        String orgId = "org";
        long caseReference = 1L;
        String email = "email";

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .useEmailAddress(VerticalYesNo.YES)
            .organisationId(orgId)
            .originalEmailAddress(email)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        assertThat(legalRepresentativeOrganisationContactDetails.getEmailAddress()).isEqualTo(email);
    }

    @Test
    void save_WithExistingLegalRep_DoesNotCreate() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .useEmailAddress(VerticalYesNo.YES)
            .organisationId(orgId)
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            new ClaimPartyContactDetailsEntity();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(orgId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        legalRepresentativePageService.save(caseReference, legalRepresentativeDetails);

        // then
        assertEquals(YesOrNo.YES, legalRepresentativeOrganisationContactDetails.getContactDetailsCorrectConfirmation());
        verify(securityContextService, never()).getCurrentUserId();
    }

    @Test
    void save_WithNoExistingLegalRepOrg_ThrowsException() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .organisationId(orgId)
            .build();

        // when / then
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> legalRepresentativePageService.save(caseReference, legalRepresentativeDetails)
        )).hasMessage("Cannot find LegalRepresentativeOrganisationContactDetails");
    }

    @Test
    void retrieveLegalRepresentativeDetails_WithExistingEmail_UsesEntityEmail() {
        // given
        String organisationId = "org";
        long caseReference = 1L;
        String email = "email1";

        AddressEntity addressEntity = AddressEntity.builder()
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            ClaimPartyContactDetailsEntity.builder()
                .emailAddress(email)
                .address(addressEntity)
                .build();

        AddressUK addressUK = AddressUK.builder().build();
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .legalRepresentativeOrganisationAddress(addressUK)
            .build();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        when(addressMapper.toAddressUK(addressEntity)).thenReturn(addressUK);

        // when
        LegalRepresentativeDetails actual = legalRepresentativePageService.retrieveLegalRepresentativeDetails(
            organisationId, caseReference,
            legalRepresentativeDetails
        );

        // then
        assertEquals(email, actual.getOriginalEmailAddress());
        verify(securityContextService, never()).getCurrentUserDetails();
        verify(addressFormatter).formatMediumAddress(addressUK, BR_DELIMITER);
    }

    @Test
    void retrieveLegalRepresentativeDetails_WithNoExistingEmail_UsesUserEmail() {
        // given
        String organisationId = "org";
        long caseReference = 1L;
        String email = "email1";

        AddressEntity addressEntity = AddressEntity.builder()
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            ClaimPartyContactDetailsEntity.builder()
                .address(addressEntity)
                .build();
        AddressUK addressUK = AddressUK.builder().build();
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .legalRepresentativeOrganisationAddress(addressUK)
            .build();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        UserInfo userInfo = UserInfo.builder().sub(email).build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(addressMapper.toAddressUK(addressEntity)).thenReturn(addressUK);

        // when
        LegalRepresentativeDetails actual = legalRepresentativePageService.retrieveLegalRepresentativeDetails(
            organisationId, caseReference,
            legalRepresentativeDetails
        );

        // then
        assertEquals(email, actual.getOriginalEmailAddress());
        verify(addressFormatter).formatMediumAddress(addressUK, BR_DELIMITER);
    }

    @Test
    void retrieveLegalRepresentativeDetails_WithExistingAddress_SetsAddressFoundToYes() {
        // given
        String organisationId = "org";
        long caseReference = 1L;

        AddressEntity addressEntity = AddressEntity.builder()
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            ClaimPartyContactDetailsEntity.builder()
                .emailAddress("email")
                .address(addressEntity)
                .build();

        AddressUK addressUK = AddressUK.builder().build();
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .legalRepresentativeOrganisationAddress(addressUK)
            .build();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        when(addressMapper.toAddressUK(addressEntity)).thenReturn(addressUK);

        // when
        LegalRepresentativeDetails actual = legalRepresentativePageService.retrieveLegalRepresentativeDetails(
            organisationId, caseReference,
            legalRepresentativeDetails
        );

        // then
        assertEquals(YesOrNo.YES, actual.getOrganisationAddressFound());
        verify(securityContextService, never()).getCurrentUserDetails();
        verify(addressFormatter).formatMediumAddress(addressUK, BR_DELIMITER);
    }


    @Test
    void retrieveLegalRepresentativeDetails_WithNoExistingAddress_SetsAddressFoundToNo() {
        // given
        String organisationId = "org";
        long caseReference = 1L;

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            ClaimPartyContactDetailsEntity.builder()
                .emailAddress("email")
                .build();

        AddressUK addressUK = AddressUK.builder().build();
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .legalRepresentativeOrganisationAddress(addressUK)
            .build();

        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        LegalRepresentativeDetails actual = legalRepresentativePageService.retrieveLegalRepresentativeDetails(
            organisationId, caseReference,
            legalRepresentativeDetails
        );

        // then
        assertEquals(YesOrNo.NO, actual.getOrganisationAddressFound());
        verify(securityContextService, never()).getCurrentUserDetails();
        verify(addressFormatter, never()).formatMediumAddress(any(AddressUK.class), eq(BR_DELIMITER));
        verify(addressMapper, never()).toAddressUK(any(AddressEntity.class));
    }

    @Test
    void retrieveLegalRepresentativeDetails_WithNoLegalRepDetails_CreatesNew() {
        // given
        String organisationId = "org";
        long caseReference = 1L;

        AddressEntity addressEntity = AddressEntity.builder()
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            ClaimPartyContactDetailsEntity.builder()
                .emailAddress("email")
                .address(addressEntity)
                .build();
        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisationContactDetails));

        // when
        LegalRepresentativeDetails actual = legalRepresentativePageService.retrieveLegalRepresentativeDetails(
            organisationId, caseReference,
            null
        );

        // then
        assertEquals(YesOrNo.NO, actual.getOrganisationAddressFound());
        verify(securityContextService, never()).getCurrentUserDetails();
        verify(addressFormatter, never()).formatMediumAddress(any(AddressUK.class), eq(BR_DELIMITER));
    }

    @Test
    void retrieveLegalRepresentativeDetails_WithNoExistingLegalRepOrg_ThrowsException() {
        // given
        String orgId = "org";
        long caseReference = 1L;

        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .build();

        // when / then
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> legalRepresentativePageService.retrieveLegalRepresentativeDetails(orgId, caseReference,
                                                                                    legalRepresentativeDetails)
        )).hasMessage("Cannot find LegalRepresentativeOrganisationContactDetails");
    }

}
