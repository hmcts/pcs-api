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
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativePartyLinkServiceTest {

    @InjectMocks
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @Mock
    private LegalRepresentativeRepository legalRepresentativeRepository;

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
    private ArgumentCaptor<LegalRepresentativeEntity> legalRepresentativeEntityCaptor;

    @Test
    void linkLegalRepresentativeToParty_WithPartyAndNonExistingLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";
        String organisationId = "ORG-123";
        String name = "name";
        String familyName = "familyName";
        String email = "email";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
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

        when(organisationDetailsService.getOrganisationDetails(userUid.toString())).thenReturn(organisationDetails);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(userInfo.getName()).thenReturn(name);
        when(userInfo.getFamilyName()).thenReturn(familyName);
        when(userInfo.getSub()).thenReturn(email);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(legalRepresentativeRepository.findByIdamId(userUid)).thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        );

        // then
        verify(legalRepresentativeRepository).saveAndFlush(legalRepresentativeEntityCaptor.capture());

        LegalRepresentativeEntity actual = legalRepresentativeEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(userUid, actual.getIdamId());
        assertEquals(name, actual.getFirstName());
        assertEquals(familyName, actual.getLastName());
        assertEquals(email, actual.getEmail());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeList().getFirst().getParty());
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

        LegalRepresentativeEntity legalRepresentative = LegalRepresentativeEntity.builder()
            .idamId(userUid)
            .build();

        when(organisationDetailsService.getOrganisationDetails(userUid.toString())).thenReturn(organisationDetails);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeRepository.findByOrganisationId(organisationId))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        );

        // then
        verify(legalRepresentativeRepository).saveAndFlush(legalRepresentativeEntityCaptor.capture());

        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
        verify(organisationDetailsService, never()).getOrganisationAddress(organisationDetails);
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        LegalRepresentativeEntity actual = legalRepresentativeEntityCaptor.getValue();

        assertEquals(userUid, actual.getIdamId());
        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithLegalRepAlreadyLinkedToParty_ThrowsException() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationId = "ORG-123";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
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

        when(organisationDetailsService.getOrganisationDetails(userUid.toString())).thenReturn(organisationDetails);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
            organisationId,
            partyId
        ))
            .thenReturn(true);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        )).isInstanceOf(LegalRepresentativeAlreadyLinkedToPartyException.class)
            .hasMessage("Legal Representative or organisation already linked to Party [" + partyId + "]");

        verify(organisationDetailsService, never()).getOrganisationName(anyString());
        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
        verify(organisationDetailsService, never()).getOrganisationAddress(anyString());
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeRepository, never()).saveAndFlush(any());
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
            userInfo
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(organisationDetailsService, never()).getOrganisationDetails(anyString());
        verify(organisationDetailsService, never()).getOrganisationName(anyString());
        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
        verify(organisationDetailsService, never()).getOrganisationAddress(anyString());
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeRepository, never()).saveAndFlush(any());

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
            userInfo
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(organisationDetailsService, never()).getOrganisationDetails(anyString());
        verify(organisationDetails, never()).getName();
        verify(organisationDetailsService, never()).getOrganisationAddress(organisationDetails);
        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeRepository, never()).saveAndFlush(any());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLinkedLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";
        String name = "name";
        String familyName = "familyName";
        String email = "email";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
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

        ClaimPartyLegalRepresentativeEntity claimPartyLegalRepresentativeEntity =
            ClaimPartyLegalRepresentativeEntity.builder()
            .active(YesOrNo.NO)
            .party(partyEntity)
                .endDate(Instant.now())
            .build();

        LegalRepresentativeEntity existingLinkedLegalRep = LegalRepresentativeEntity.builder()
                .claimPartyLegalRepresentativeList(List.of(claimPartyLegalRepresentativeEntity))
            .build();

        when(legalRepresentativeRepository.isPartyLinkedToLegalRepresentativeAndActive(partyId))
            .thenReturn(Optional.of(existingLinkedLegalRep));

        when(organisationDetailsService.getOrganisationDetails(userUid.toString())).thenReturn(organisationDetails);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(userInfo.getName()).thenReturn(name);
        when(userInfo.getFamilyName()).thenReturn(familyName);
        when(userInfo.getSub()).thenReturn(email);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(legalRepresentativeRepository.findByIdamId(userUid)).thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        );

        // then
        verify(legalRepresentativeRepository, times(2)).saveAndFlush(legalRepresentativeEntityCaptor.capture());

        List<LegalRepresentativeEntity> actualList = legalRepresentativeEntityCaptor.getAllValues();

        LegalRepresentativeEntity unlinked = actualList.getFirst();
        ClaimPartyLegalRepresentativeEntity unlinkedClaimParty =
            unlinked.getClaimPartyLegalRepresentativeList().getFirst();

        assertEquals(YesOrNo.NO, unlinkedClaimParty.getActive());
        assertNotNull(unlinkedClaimParty.getEndDate());

        LegalRepresentativeEntity actual = actualList.get(1);

        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(userUid, actual.getIdamId());
        assertEquals(name, actual.getFirstName());
        assertEquals(familyName, actual.getLastName());
        assertEquals(email, actual.getEmail());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeList().getFirst().getParty());
    }

}
