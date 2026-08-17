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
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativePartyLinkServiceTest {

    private static final String LEGAL_REP_EMAIL = "solicitor@example.com";

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
    private CaseRoleAssignmentService caseRoleAssignmentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AddressUK addressUK;

    @Mock
    private AddressEntity addressEntity;

    @Mock
    private OrganisationDetailsResponse organisationDetails;

    @Captor
    private ArgumentCaptor<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisationEntityCaptor;

    private static final String ORG_PROFILE_ID = "SOLICITOR_PROFILE";

    @Test
    void linkLegalRepresentativeToParty_WithPartyAndNonExistingLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
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

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
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
            LEGAL_REP_EMAIL,
            organisationDetails
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        LegalRepresentativeOrganisationContactDetailsEntity actualContactDetails =
            actual.getLegalRepresentativeOrganisationContactDetails();

        assertEquals(addressEntity, actualContactDetails.getAddress());
        assertEquals(LEGAL_REP_EMAIL, actualContactDetails.getEmailAddress());
        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(ORG_PROFILE_ID, actual.getOrganisationProfileId());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeOrganisationList().getFirst().getParty());
        verify(caseRoleAssignmentService, never()).revokeRasRole(anyLong(), anyString(), any(UserRole.class));
        verify(notificationService).sendNoticeOfChangeCompletedEmailNotification(partyEntity);
        verify(notificationService, never())
            .sendNoticeOfChangeNoLongerRepresentingEmailNotification(any(), any());
        verify(notificationService).sendNoticeOfChangeCompleteLegalRepEmailNotification(actual, partyEntity);
    }

    @Test
    void linkLegalRepresentativeToParty_WithLinkedDefendant_RevokesDefendantRole() {
        // given
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

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference)).thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            LEGAL_REP_EMAIL,
            organisationDetails
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


        ClaimPartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation =
            ClaimPartyLegalRepresentativeOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        LegalRepresentativeOrganisationEntity legalRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .claimPartyLegalRepresentativeOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .legalRepresentativeOrganisationContactDetails(
                LegalRepresentativeOrganisationContactDetailsEntity.builder().build())
            .build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            LEGAL_REP_EMAIL,
            organisationDetails
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        verify(organisationDetailsService, never()).getOrganisationAddress(organisationDetails);
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(LEGAL_REP_EMAIL,
                     actual.getLegalRepresentativeOrganisationContactDetails().getEmailAddress());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLegalRepresentativeWithOrgDetails_SavesNewEntity() {
        // given
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

        ClaimPartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation =
            ClaimPartyLegalRepresentativeOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        LegalRepresentativeOrganisationEntity legalRepresentative = LegalRepresentativeOrganisationEntity.builder()
            .organisationName(organisationName)
            .organisationId(organisationId)
            .claimPartyLegalRepresentativeOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            LEGAL_REP_EMAIL,
            organisationDetails
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLegalRepresentativeForOrg_SavesNewEntity() {
        // given
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

        ClaimPartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation =
            ClaimPartyLegalRepresentativeOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation =
            LegalRepresentativeOrganisationEntity.builder()
            .organisationName(organisationName)
            .organisationId(organisationId)
            .claimPartyLegalRepresentativeOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(legalRepresentativeOrganisationRepository
                 .findByOrganisationIdAndCaseReference(organisationId, caseReference))
            .thenReturn(Optional.of(legalRepresentativeOrganisation));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            LEGAL_REP_EMAIL,
            organisationDetails
        );

        // then
        verify(legalRepresentativeOrganisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(organisationId, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeOrganisationList().getFirst().getParty());
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
        )).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            LEGAL_REP_EMAIL,
            organisationDetails
        )).isInstanceOf(LegalRepresentativeAlreadyLinkedToPartyException.class)
            .hasMessage("Legal Representative or organisation already linked to Party [" + partyId + "]");

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(notificationService, never()).sendNoticeOfChangeCompletedEmailNotification(any());
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
            LEGAL_REP_EMAIL,
            organisationDetails
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

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
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            LEGAL_REP_EMAIL,
            organisationDetails
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(legalRepresentativeOrganisationRepository, never()).save(any());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLinkedLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        UUID partyId2 = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();

        PartyEntity partyEntity2 = PartyEntity.builder()
            .id(partyId2)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity2)
                                                .build()))
                                .build()
            )).build();

        ClaimPartyLegalRepresentativeOrganisationEntity claimPartyLegalRepresentativeEntity =
            ClaimPartyLegalRepresentativeOrganisationEntity.builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        ClaimPartyLegalRepresentativeOrganisationEntity claimPartyLegalRepresentativeEntity2 =
            ClaimPartyLegalRepresentativeOrganisationEntity.builder()
                .active(YesOrNo.YES)
                .party(partyEntity2)
                .build();

        LegalRepresentativeOrganisationEntity existingLinkedLegalRep = LegalRepresentativeOrganisationEntity.builder()
            .claimPartyLegalRepresentativeOrganisationList(List.of(claimPartyLegalRepresentativeEntity,
                                                                   claimPartyLegalRepresentativeEntity2))
            .build();

        when(legalRepresentativeOrganisationRepository
                 .findByPartyLinkedToLegalRepresentativeOrganisationAndActive(partyId))
            .thenReturn(Optional.of(existingLinkedLegalRep));
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(caseReference, partyId.toString(),
                                                                           LEGAL_REP_EMAIL, organisationDetails);

        // then
        verify(legalRepresentativeOrganisationRepository, times(2))
            .save(legalRepresentativeOrganisationEntityCaptor.capture());

        List<LegalRepresentativeOrganisationEntity> actualList = legalRepresentativeOrganisationEntityCaptor
            .getAllValues();

        LegalRepresentativeOrganisationEntity unlinked = actualList.getFirst();
        ClaimPartyLegalRepresentativeOrganisationEntity unlinkedClaimParty =
            unlinked.getClaimPartyLegalRepresentativeOrganisationList().getFirst();

        assertEquals(YesOrNo.NO, unlinkedClaimParty.getActive());
        assertNotNull(unlinkedClaimParty.getEndDate());

        ClaimPartyLegalRepresentativeOrganisationEntity unAffectedClaimParty =
            unlinked.getClaimPartyLegalRepresentativeOrganisationList().get(1);

        assertEquals(YesOrNo.YES, unAffectedClaimParty.getActive());
        assertNull(unAffectedClaimParty.getEndDate());

        LegalRepresentativeOrganisationEntity actual = actualList.get(1);
        LegalRepresentativeOrganisationContactDetailsEntity actualContactDetails =
            actual.getLegalRepresentativeOrganisationContactDetails();

        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(addressEntity, actualContactDetails.getAddress());
        assertEquals(ORG_PROFILE_ID, actual.getOrganisationProfileId());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeOrganisationList().getFirst().getParty());
        verify(notificationService)
            .sendNoticeOfChangeNoLongerRepresentingEmailNotification(existingLinkedLegalRep, partyEntity);
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLegalRepresentativeNotOnCase_SavesNewLegalRepresentativeEntity() {
        // given
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

        when(legalRepresentativeOrganisationRepository
                 .findByPartyLinkedToLegalRepresentativeOrganisationAndActive(partyId))
            .thenReturn(Optional.empty());

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(caseReference, partyId.toString(),
                                                                           LEGAL_REP_EMAIL, organisationDetails);

        // then
        verify(legalRepresentativeOrganisationRepository, times(1))
            .save(legalRepresentativeOrganisationEntityCaptor.capture());

        LegalRepresentativeOrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();
        LegalRepresentativeOrganisationContactDetailsEntity actualContactDetails =
            actual.getLegalRepresentativeOrganisationContactDetails();

        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(ORG_PROFILE_ID, actual.getOrganisationProfileId());
        assertEquals(addressEntity, actualContactDetails.getAddress());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeOrganisationList().getFirst().getParty());
    }

}
