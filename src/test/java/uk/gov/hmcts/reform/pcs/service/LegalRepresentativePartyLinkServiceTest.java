package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativePartyLinkServiceTest {

    @InjectMocks
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @Mock
    private LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private UserInfo userInfo;

    @Mock
    private AddressUK addressUK;

    @Mock
    private AddressEntity addressEntity;

    @Mock
    private OrganisationDetailsResponse organisationDetails;

    @Captor
    private ArgumentCaptor<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisationEntityCaptor;

    @Test
    void linkLegalRepresentativeToParty_WithPartyAndNonExistingLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";
        String organisationId = "ORG-123";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(organisationId,
                                                                                            caseReference))
            .thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo,
            organisationDetails
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithPartyAndExistingLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationId = "ORG-123";
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder()
                         .caseReference(caseReference)
                         .build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();


        PartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation =
            PartyLegalRepresentativeOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        LegalRepresentativeOrganisationEntity legalRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .partyLegalRepresentativeOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo,
            organisationDetails
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        verify(organisationDetailsService, never()).getOrganisationAddress(organisationDetails);
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLegalRepresentativeWithOrgDetails_SavesNewEntity() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationId = "ORG-123";
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        PartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation =
            PartyLegalRepresentativeOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        LegalRepresentativeOrganisationEntity legalRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationName(organisationName)
            .organisationId(organisationId)
            .partyLegalRepresentativeOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo,
            organisationDetails
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());
        verify(organisationDetailsService, never()).getOrganisationAddress(organisationDetails);
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithLegalRepAlreadyLinkedToParty_ThrowsException() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationId = "ORG-123";

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(legalRepresentativeOrganisationRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
            organisationId,
            partyId
        ))
            .thenReturn(true);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo,
            organisationDetails
        )).isInstanceOf(LegalRepresentativeAlreadyLinkedToPartyException.class)
            .hasMessage("Legal Representative or organisation already linked to Party [" + partyId + "]");

        verify(organisationDetailsService, never()).getOrganisationName(anyString());
        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
        verify(organisationDetailsService, never()).getOrganisationAddress(anyString());
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeOrganisationRepository, never()).save(any());
    }

    @Test
    void linkLegalRepresentativeToParty_WithNoPartyFound_ThrowsException() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId2)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo,
            organisationDetails
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(organisationDetailsService, never()).getOrganisationName(anyString());
        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
        verify(organisationDetailsService, never()).getOrganisationAddress(anyString());
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeOrganisationRepository, never()).save(any());

    }

    @Test
    void linkLegalRepresentativeToParty_WithNoDefendantPartyFound_ThrowsException() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.UNDERLESSEE_OR_MORTGAGEE)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo,
            organisationDetails
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(organisationDetails, never()).getName();
        verify(organisationDetailsService, never()).getOrganisationAddress(organisationDetails);
        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeOrganisationRepository, never()).save(any());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLinkedLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(caseReference, partyId.toString(), userInfo,
                                                                           organisationDetails);

        // then
        verify(legalRepresentativeOrganisationRepository, times(1))
            .save(legalRepresentativeOrganisationEntityCaptor.capture());

        List<LegalRepresentativeOrganisationEntity> actualList = legalRepresentativeOrganisationEntityCaptor
            .getAllValues();

        LegalRepresentativeOrganisationEntity actual = actualList.getFirst();

        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLegalRepresentativeNotOnCase_SavesNewLegalRepresentativeEntity() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder().caseReference(2L).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(caseReference, partyId.toString(), userInfo,
                                                                           organisationDetails);

        // then
        verify(legalRepresentativeOrganisationRepository, times(1))
            .save(legalRepresentativeOrganisationEntityCaptor.capture());

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

}
