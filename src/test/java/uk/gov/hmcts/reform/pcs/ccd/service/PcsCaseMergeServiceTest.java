package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcsCaseMergeServiceTest {

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private TenancyLicenceService tenancyLicenceService;
    @Mock
    private PCSCase pcsCase;

    private PcsCaseMergeService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsCaseMergeService(securityContextService,
                                            modelMapper,
                                            tenancyLicenceService);
    }

    @Test
    void shouldLeaveFieldsUnchangedWhenPatchingCaseWithNoData() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        when(pcsCase.getCaseManagementLocation()).thenReturn(null);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setPossessionGrounds(any());
    }

    @Test
    void shouldChangeFieldsWhenPatchingCase() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        AddressUK updatedPropertyAddress = mock(AddressUK.class);
        final AddressEntity updatedAddressEntity = stubAddressUKModelMapper(updatedPropertyAddress);

        when(pcsCase.getPropertyAddress()).thenReturn(updatedPropertyAddress);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setPropertyAddress(updatedAddressEntity);
    }

    @Test
    void shouldCreatePartyWithPcqIdForCurrentUser() {
        UUID userId = UUID.randomUUID();
        String expectedPcqId = UUID.randomUUID().toString();
        String expectedFirstName = "some first name";
        String expectedLastName = "some last name";

        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId);

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getGivenName()).thenReturn(expectedFirstName);
        when(userDetails.getFamilyName()).thenReturn(expectedLastName);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCaseEntity.getParties())
            .hasSize(1)
            .allSatisfy(party -> {
                assertThat(party.getIdamId()).isEqualTo(userId);
                assertThat(party.getPcqId()).isEqualTo(expectedPcqId);
                assertThat(party.getFirstName()).isEqualTo(expectedFirstName);
                assertThat(party.getLastName()).isEqualTo(expectedLastName);
            });

    }

    @Test
    void shouldUpdateExistingPartyWithPcqIdForCurrentUser() {
        final UUID userId = UUID.randomUUID();
        final String expectedPcqId = UUID.randomUUID().toString();
        final UUID existingPartyId = UUID.randomUUID();

        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId);

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(userId.toString());
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);

        PartyEntity existingPartyEntity = new PartyEntity();
        existingPartyEntity.setId(existingPartyId);
        existingPartyEntity.setIdamId(userId);

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.addParty(existingPartyEntity);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCaseEntity.getParties())
            .hasSize(1)
            .allSatisfy(
                party -> {
                    assertThat(party.getId()).isEqualTo(existingPartyId);
                    assertThat(party.getPcqId()).isEqualTo(expectedPcqId);
                }
            );

    }

    @Test
    void shouldUpdateCaseManagementLocationWhenNotNull() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        int location = 13685;

        when(pcsCase.getCaseManagementLocation()).thenReturn(location);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setCaseManagementLocation(location);
    }

    @Test
    void shouldMapWalesStandardContractGroundsToPossessionGrounds() {
        SecureContractGroundsForPossessionWales secureContractGroundsWales =
            mock(SecureContractGroundsForPossessionWales.class);
        when(pcsCase.getSecureContractGroundsForPossessionWales()).thenReturn(secureContractGroundsWales);


        Set<DiscretionaryGroundWales> discretionaryGrounds = Set.of(
                DiscretionaryGroundWales.RENT_ARREARS_SECTION_157,
                DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157);
        Set<MandatoryGroundWales> mandatoryGrounds = Set.of(
                MandatoryGroundWales.FAIL_TO_GIVE_UP_S170);
        Set<EstateManagementGroundsWales> estateManagementGrounds = Set.of(
                EstateManagementGroundsWales.BUILDING_WORKS,
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES);

        GroundsForPossessionWales groundsForPossessionWales = mock(GroundsForPossessionWales.class);
        when(groundsForPossessionWales.getDiscretionaryGroundsWales()).thenReturn(discretionaryGrounds);
        when(groundsForPossessionWales.getMandatoryGroundsWales()).thenReturn(mandatoryGrounds);
        when(groundsForPossessionWales.getEstateManagementGroundsWales()).thenReturn(estateManagementGrounds);
        when(pcsCase.getGroundsForPossessionWales()).thenReturn(groundsForPossessionWales);
        when(secureContractGroundsWales
                 .getDiscretionaryGroundsWales()).thenReturn(null);
        when(secureContractGroundsWales
                 .getMandatoryGroundsWales()).thenReturn(null);
        when(secureContractGroundsWales
                 .getEstateManagementGroundsWales()).thenReturn(null);
        when(pcsCase.getSecureOrFlexibleGroundsReasons()).thenReturn(null);

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        assertThat(pcsCaseEntity.getPossessionGrounds()).isNotNull();
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesDiscretionaryGrounds())
                .contains(
                        DiscretionaryGroundWales.RENT_ARREARS_SECTION_157.getLabel(),
                        DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157.getLabel());
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesMandatoryGrounds())
                .contains(
                        MandatoryGroundWales.FAIL_TO_GIVE_UP_S170.getLabel());
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesEstateManagementGrounds())
                .contains(
                        EstateManagementGroundsWales.BUILDING_WORKS.getLabel(),
                        EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.getLabel());
    }

    @Test
    void shouldMapWalesSecureContractGroundsToPossessionGrounds() {
        SecureContractGroundsForPossessionWales secureContractGroundsWales =
            mock(SecureContractGroundsForPossessionWales.class);
        when(pcsCase.getSecureContractGroundsForPossessionWales()).thenReturn(secureContractGroundsWales);

        EnumSet<SecureContractDiscretionaryGroundsWales> discretionaryGrounds = EnumSet.of(
                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR);
        EnumSet<SecureContractMandatoryGroundsWales> mandatoryGrounds = EnumSet.of(
                SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170);
        EnumSet<EstateManagementGroundsWales> estateManagementGrounds = EnumSet.of(
                EstateManagementGroundsWales.BUILDING_WORKS,
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES);

        GroundsForPossessionWales groundsForPossessionWales = mock(GroundsForPossessionWales.class);
        when(groundsForPossessionWales.getDiscretionaryGroundsWales()).thenReturn(null);
        when(groundsForPossessionWales.getMandatoryGroundsWales()).thenReturn(null);
        when(groundsForPossessionWales.getEstateManagementGroundsWales()).thenReturn(null);
        when(pcsCase.getGroundsForPossessionWales()).thenReturn(groundsForPossessionWales);
        when(secureContractGroundsWales
                 .getDiscretionaryGroundsWales()).thenReturn(discretionaryGrounds);
        when(secureContractGroundsWales
                 .getMandatoryGroundsWales()).thenReturn(mandatoryGrounds);
        when(secureContractGroundsWales
                 .getEstateManagementGroundsWales()).thenReturn(estateManagementGrounds);
        when(pcsCase.getSecureOrFlexibleGroundsReasons()).thenReturn(null);

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        assertThat(pcsCaseEntity.getPossessionGrounds()).isNotNull();
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesSecureContractDiscretionaryGrounds())
                .contains(
                        SecureContractDiscretionaryGroundsWales.RENT_ARREARS.getLabel(),
                        SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR.getLabel());
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesSecureContractMandatoryGrounds())
                .contains(
                        SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170.getLabel());
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesSecureContractEstateManagementGrounds())
                .contains(
                        EstateManagementGroundsWales.BUILDING_WORKS.getLabel(),
                        EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.getLabel());
    }

    @Test
    void shouldSetTenancyLicence() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        TenancyLicenceEntity tenancyLicenceEntity = mock(TenancyLicenceEntity.class);
        when(tenancyLicenceService.createTenancyLicenceEntity(pcsCase)).thenReturn(tenancyLicenceEntity);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCaseEntity.getTenancyLicence()).isEqualTo(tenancyLicenceEntity);
    }

    private AddressEntity stubAddressUKModelMapper(AddressUK addressUK) {
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(addressEntity);
        return addressEntity;
    }

}
