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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.NameAndAddress;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private OrganisationService organisationService;

    @Mock
    private LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;

    @Mock
    private UserInfo userInfo;

    @Mock
    private AddressUK addressUK;

    @Mock
    private AddressEntity addressEntity;

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
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();
        NameAndAddress nameAndAddress = new NameAndAddress(organisationName, addressUK);
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(organisationService.getNameAndAddress(userUid.toString())).thenReturn(nameAndAddress);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(organisationId,
                                                                                            caseReference))
            .thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
        verify(caseRoleAssignmentService, never()).revokeRasRole(anyLong(), anyString(), any(UserRole.class));
    }

    @Test
    void linkLegalRepresentativeToParty_WithLinkedDefendant_RevokesDefendantRole() {
        // given
        UUID userUid = UUID.randomUUID();
        UUID defendantIdamId = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";
        String organisationId = "ORG-123";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .idamId(defendantIdamId)
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
        NameAndAddress nameAndAddress = new NameAndAddress(organisationName, addressUK);
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(organisationService.getNameAndAddress(userUid.toString())).thenReturn(nameAndAddress);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference)).thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        );

        // then
        verify(caseRoleAssignmentService).revokeRasRole(
            caseReference,
            defendantIdamId.toString(),
            UserRole.DEFENDANT
        );
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
        NameAndAddress nameAndAddress = new NameAndAddress(organisationName, null);
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(organisationService.getNameAndAddress(userUid.toString())).thenReturn(nameAndAddress);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

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

        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo);

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithLegalRepAlreadyLinkedToParty_ThrowsException() {
        // given
        UUID userUid = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationId = "ORG-123";
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());

        when(legalRepresentativeOrganisationRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
            organisationId,
            partyId
        )).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        )).isInstanceOf(LegalRepresentativeAlreadyLinkedToPartyException.class)
            .hasMessage("Legal Representative or organisation already linked to Party [" + partyId + "]");

        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
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
        UUID userUid = UUID.randomUUID();
        String organisationId = "organisationId";
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(userInfo, never()).getName();
        verify(userInfo, never()).getFamilyName();
        verify(userInfo, never()).getSub();
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
        String organisationId = "organisationId";
        UUID userUid = UUID.randomUUID();
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            userInfo
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

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
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        PartyLegalRepresentativeOrganisationEntity claimPartyLegalRepresentativeEntity =
            PartyLegalRepresentativeOrganisationEntity.builder()
                .active(YesOrNo.NO)
                .party(partyEntity)
                .endDate(Instant.now())
                .build();

        LegalRepresentativeOrganisationEntity existingLinkedLegalRep = LegalRepresentativeOrganisationEntity.builder()
            .partyLegalRepresentativeOrganisationList(List.of(claimPartyLegalRepresentativeEntity))
            .build();
        String organisationId = "organisationId";
        NameAndAddress nameAndAddress = new NameAndAddress(organisationName, addressUK);
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(organisationService.getNameAndAddress(userUid.toString())).thenReturn(nameAndAddress);

        when(legalRepresentativeOrganisationRepository
                 .findByPartyLinkedToLegalRepresentativeOrganisationAndActive(partyId))
            .thenReturn(Optional.of(existingLinkedLegalRep));

        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(caseReference, partyId.toString(), userInfo);

        // then
        verify(legalRepresentativeOrganisationRepository, times(2))
            .save(legalRepresentativeOrganisationEntityCaptor.capture());

        List<LegalRepresentativeOrganisationEntity> actualList = legalRepresentativeOrganisationEntityCaptor
            .getAllValues();

        LegalRepresentativeOrganisationEntity unlinked = actualList.getFirst();
        PartyLegalRepresentativeOrganisationEntity unlinkedClaimParty =
            unlinked.getPartyLegalRepresentativeOrganisationList().getFirst();

        assertEquals(YesOrNo.NO, unlinkedClaimParty.getActive());
        assertNotNull(unlinkedClaimParty.getEndDate());

        LegalRepresentativeOrganisationEntity actual = actualList.get(1);

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
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        String organisationId = "organisationId";
        NameAndAddress nameAndAddress = new NameAndAddress(organisationName, addressUK);
        when(organisationService.getOrganisationId(userUid.toString())).thenReturn(organisationId);
        when(organisationService.getNameAndAddress(userUid.toString())).thenReturn(nameAndAddress);
        when(legalRepresentativeOrganisationRepository
                 .findByPartyLinkedToLegalRepresentativeOrganisationAndActive(partyId))
            .thenReturn(Optional.empty());
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(caseReference, partyId.toString(), userInfo);

        // then
        verify(legalRepresentativeOrganisationRepository, times(1))
            .save(legalRepresentativeOrganisationEntityCaptor.capture());

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

}
