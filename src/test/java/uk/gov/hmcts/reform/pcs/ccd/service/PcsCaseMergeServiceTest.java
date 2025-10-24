package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.mapper.DefendantMapper;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.MapperConfig;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
    private DefendantMapper defendantMapper;

    private PcsCaseMergeService underTest;

    @BeforeEach
    void setUp() {
        MapperConfig config = new MapperConfig();
        modelMapper = spy(config.modelMapper());
        underTest = new PcsCaseMergeService(securityContextService, modelMapper, tenancyLicenceService,
                                            defendantMapper);
    }

    @Test
    void shouldLeaveFieldsUnchangedWhenPatchingCaseWithNoData() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        when(pcsCase.getCaseManagementLocation()).thenReturn(null);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setTenancyLicence(any());
        verify(pcsCaseEntity).setPossessionGrounds(any());
    }

    @Test
    void shouldChangeFieldsWhenPatchingCase() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
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
        PCSCase pcsCase = mock(PCSCase.class);

        UUID userId = UUID.randomUUID();
        UUID expectedPcqId = UUID.randomUUID();
        String expectedForename = "some forename";
        String expectedSurname = "some surname";

        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId.toString());

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getGivenName()).thenReturn(expectedForename);
        when(userDetails.getFamilyName()).thenReturn(expectedSurname);
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
                assertThat(party.getForename()).isEqualTo(expectedForename);
                assertThat(party.getSurname()).isEqualTo(expectedSurname);
            });

    }

    @Test
    void shouldUpdateExistingPartyWithPcqIdForCurrentUser() {
        PCSCase pcsCase = mock(PCSCase.class);

        final UUID userId = UUID.randomUUID();
        final UUID expectedPcqId = UUID.randomUUID();
        final UUID existingPartyId = UUID.randomUUID();

        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId.toString());

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
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        int location = 13685;

        when(pcsCase.getCaseManagementLocation()).thenReturn(location);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setCaseManagementLocation(location);
    }

    @Test
    void shouldUpdatePreActionProtocolCompletedWhenNotNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        VerticalYesNo preActionProtocolCompleted = VerticalYesNo.YES;

        when(pcsCase.getPreActionProtocolCompleted()).thenReturn(preActionProtocolCompleted);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setPreActionProtocolCompleted(preActionProtocolCompleted.toBoolean());
    }

    @Test
    void shouldMergeClaimantTypeWhenAvailable() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(ClaimantType.COMMUNITY_LANDLORD.name())
                .label(ClaimantType.COMMUNITY_LANDLORD.getLabel())
                .build())
            .build();

        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setClaimantType(ClaimantType.COMMUNITY_LANDLORD);
    }

    @Test
    void shouldSkipClaimantTypeWhenNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        when(pcsCase.getClaimantType()).thenReturn(null);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity, never()).setClaimantType(any());
    }


    @Test
    void shouldMergeAllClaimantTypes() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(ClaimantType.PRIVATE_LANDLORD.name())
                .label(ClaimantType.PRIVATE_LANDLORD.getLabel())
                .build())
            .build();

        when(pcsCase.getClaimantType()).thenReturn(claimantTypeList);

        // When
        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        // Then
        verify(pcsCaseEntity).setClaimantType(ClaimantType.PRIVATE_LANDLORD);
    }

    @Test
    void shouldMapWalesPossessionGroundsToJsonb() {
        PCSCase pcsCase = mock(PCSCase.class);

        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds = Set.of(
                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR);
        Set<SecureContractMandatoryGroundsWales> mandatoryGrounds = Set.of(
                SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170);
        Set<EstateManagementGroundsWales> estateManagementGrounds = Set.of(
                EstateManagementGroundsWales.BUILDING_WORKS,
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES);

        when(pcsCase.getSecureContractDiscretionaryGroundsWales()).thenReturn(discretionaryGrounds);
        when(pcsCase.getSecureContractMandatoryGroundsWales()).thenReturn(mandatoryGrounds);
        when(pcsCase.getEstateManagementGroundsWales()).thenReturn(estateManagementGrounds);
        when(pcsCase.getSecureOrFlexibleDiscretionaryGrounds()).thenReturn(null);
        when(pcsCase.getSecureOrFlexibleMandatoryGrounds()).thenReturn(null);
        when(pcsCase.getSecureOrFlexibleDiscretionaryGroundsAlt()).thenReturn(null);
        when(pcsCase.getSecureOrFlexibleMandatoryGroundsAlt()).thenReturn(null);
        when(pcsCase.getSecureOrFlexibleGroundsReasons()).thenReturn(null);

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        underTest.mergeCaseData(pcsCaseEntity, pcsCase);

        assertThat(pcsCaseEntity.getPossessionGrounds()).isNotNull();
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesDiscretionaryGrounds())
                .contains(
                        SecureContractDiscretionaryGroundsWales.RENT_ARREARS.getLabel(),
                        SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR.getLabel());
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesMandatoryGrounds())
                .contains(
                        SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170.getLabel());
        assertThat(pcsCaseEntity.getPossessionGrounds().getWalesEstateManagementGrounds())
                .contains(
                        EstateManagementGroundsWales.BUILDING_WORKS.getLabel(),
                        EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.getLabel());
    }

    private AddressEntity stubAddressUKModelMapper(AddressUK addressUK) {
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(addressEntity);
        return addressEntity;
    }

}
