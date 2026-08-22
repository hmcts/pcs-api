package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.ConflictOfInterestException;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.util.RevokeAccessHelper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativePartyLinkServiceTest {

    private static final String ORGANISATION_ID = "ORG-123";

    @InjectMocks
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private ClaimPartyContactDetailsRepository claimPartyContactDetailsRepository;

    @Mock
    private RevokeAccessHelper revokeAccessHelper;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private AddressUK addressUK;

    @Mock
    private AddressEntity addressEntity;

    @Mock
    private OrganisationDetailsResponse organisationDetails;

    @Captor
    private ArgumentCaptor<OrganisationEntity> legalRepresentativeOrganisationEntityCaptor;

    private static final String ORG_PROFILE_ID = "SOLICITOR_PROFILE";

    private static final Clock FIXED_UTC_CLOCK = Clock.fixed(
        Instant.parse("2026-04-22T21:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        legalRepresentativePartyLinkService = new LegalRepresentativePartyLinkService(
            pcsCaseService,
            organisationRepository,
            claimPartyContactDetailsRepository,
            organisationDetailsService,
            addressMapper,
            revokeAccessHelper,
            FIXED_UTC_CLOCK
        );
    }

    @Test
    void linkLegalRepresentativeToParty_WithPartyAndNonExistingLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();

        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);
        when(organisationDetails.getOrgProfileId()).thenReturn(ORG_PROFILE_ID);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(organisationRepository.findByOrganisationId(ORGANISATION_ID))
            .thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        );

        // then
        verify(organisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        OrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        ClaimPartyContactDetailsEntity actualContactDetails =
            actual.getClaimPartyContactDetails().getFirst();

        assertThat(actual.getCreatedDate()).isEqualTo(LocalDateTime.now(FIXED_UTC_CLOCK));
        assertEquals(addressEntity, actualContactDetails.getAddress());
        assertEquals(ORGANISATION_ID, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(ORG_PROFILE_ID, actual.getOrganisationProfileId());
        assertEquals(partyEntity, actual.getClaimPartyOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithLinkedDefendant_RevokesDefendantRole() {
        // given
        UUID defendantIdamId = UUID.randomUUID();
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";


        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .idamId(defendantIdamId)
            .build();

        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        when(organisationDetails.getName()).thenReturn(organisationName);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(organisationRepository
                 .findByOrganisationId(ORGANISATION_ID)).thenReturn(Optional.empty());

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        );

        // then
        verify(revokeAccessHelper).revokeDefendantsAccessToRespondToClaim(pcsCaseEntity, partyEntity);
    }

    @Test
    void linkLegalRepresentativeToParty_WithPartyAndExistingLegalRepresentative_SavesNewLegalRepresentativeEntity() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder()
                         .caseReference(caseReference)
                         .build())
            .build();

        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();


        ClaimPartyOrganisationEntity partyLegalRepresentativeOrganisation =
            ClaimPartyOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        OrganisationEntity legalRepresentative = OrganisationEntity.builder()
            .claimPartyOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        ClaimPartyContactDetailsEntity contactDetails =
            ClaimPartyContactDetailsEntity.builder().build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationRepository
                 .findByOrganisationId(ORGANISATION_ID))
            .thenReturn(Optional.of(legalRepresentative));
        when(claimPartyContactDetailsRepository
                 .findByOrganisationIdAndCaseReference(ORGANISATION_ID, caseReference))
            .thenReturn(Optional.of(contactDetails));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        );

        // then
        verify(organisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        OrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(ORGANISATION_ID, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getClaimPartyOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithNewCaseAndExistingLegalRepresentative_SavesNewContactDetails() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder()
                         .caseReference(caseReference)
                         .build())
            .build();

        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        ClaimPartyOrganisationEntity partyLegalRepresentativeOrganisation =
            ClaimPartyOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        OrganisationEntity legalRepresentative = OrganisationEntity.builder()
            .claimPartyOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        when(organisationDetailsService.getOrganisationAddress(organisationDetails))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);
        when(organisationDetails.getName()).thenReturn(organisationName);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationRepository
                 .findByOrganisationId(ORGANISATION_ID))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        );

        // then
        verify(organisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());

        verify(organisationDetailsService).getOrganisationAddress(organisationDetails);
        verify(addressMapper).toAddressEntityAndNormalise(addressUK);

        OrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(ORGANISATION_ID, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getClaimPartyOrganisationList().getFirst().getParty());

        ClaimPartyContactDetailsEntity actualContactDetails =
            actual.getClaimPartyContactDetails().getFirst();

        assertEquals(pcsCaseEntity, actualContactDetails.getPcsCase());
        assertEquals(addressEntity, actualContactDetails.getAddress());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLegalRepresentativeWithOrgDetails_SavesNewEntity() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()
                                    )
                                )
                                .build()
            )).build();

        ClaimPartyOrganisationEntity partyLegalRepresentativeOrganisation =
            ClaimPartyOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        OrganisationEntity legalRepresentative = OrganisationEntity.builder()
            .organisationName(organisationName)
            .organisationId(ORGANISATION_ID)
            .claimPartyOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationRepository
                 .findByOrganisationId(ORGANISATION_ID))
            .thenReturn(Optional.of(legalRepresentative));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        );

        // then
        verify(organisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        OrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(ORGANISATION_ID, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getClaimPartyOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithExistingLegalRepresentativeForOrg_SavesNewEntity() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        String organisationName = "orgName";

        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                    ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();

        ClaimPartyOrganisationEntity partyLegalRepresentativeOrganisation =
            ClaimPartyOrganisationEntity
                .builder()
                .active(YesOrNo.YES)
                .party(partyEntity)
                .build();

        OrganisationEntity legalRepresentativeOrganisation =
            OrganisationEntity.builder()
            .organisationName(organisationName)
            .organisationId(ORGANISATION_ID)
            .claimPartyOrganisationList(List.of(partyLegalRepresentativeOrganisation))
            .build();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationRepository
                 .findByOrganisationId(ORGANISATION_ID))
            .thenReturn(Optional.of(legalRepresentativeOrganisation));

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        );

        // then
        verify(organisationRepository).save(legalRepresentativeOrganisationEntityCaptor.capture());
        verify(addressMapper, never()).toAddressEntityAndNormalise(addressUK);

        OrganisationEntity actual = legalRepresentativeOrganisationEntityCaptor.getValue();

        assertEquals(ORGANISATION_ID, actual.getOrganisationId());
        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(partyEntity, actual.getClaimPartyOrganisationList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithLegalRepAlreadyLinkedToParty_ThrowsException() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();

        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);
        when(organisationRepository.isOrganisationLinkedToPartyAndActive(
            ORGANISATION_ID,
            partyId
        )).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        )).isInstanceOf(LegalRepresentativeAlreadyLinkedToPartyException.class)
            .hasMessage("Legal Representative or organisation already linked to Party [" + partyId + "]");

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(organisationRepository, never()).save(any());
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

        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.DEFENDANT)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            )).build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(organisationRepository, never()).save(any());

    }

    @Test
    void linkLegalRepresentativeToParty_WithNoDefendantPartyFound_ThrowsException() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();
        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.UNDERLESSEE_OR_MORTGAGEE)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            ))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        )).isInstanceOf(PartyNotFoundException.class)
            .hasMessage("Unable to find Party with Id [" + partyId + "]");

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(organisationRepository, never()).save(any());
    }

    @Test
    void linkLegalRepresentativeToParty_FromTheSameOrganisation_ThrowsException() {
        // given
        long caseReference = 1L;
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .build();
        PartyEntity claimantPartyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(PcsCaseEntity.builder().caseReference(caseReference).build())
            .organisationId(ORGANISATION_ID)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .claims(List.of(ClaimEntity.builder()
                                .claimParties(
                                    List.of(ClaimPartyEntity.builder()
                                                .role(PartyRole.CLAIMANT)
                                                .party(claimantPartyEntity)
                                                .build(),
                                            ClaimPartyEntity.builder()
                                                .role(PartyRole.UNDERLESSEE_OR_MORTGAGEE)
                                                .party(partyEntity)
                                                .build()))
                                .build()
            ))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetails.getOrganisationIdentifier()).thenReturn(ORGANISATION_ID);

        // when / then
        assertThatThrownBy(() -> legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            caseReference,
            partyId.toString(),
            organisationDetails
        )).isInstanceOf(ConflictOfInterestException.class)
            .hasMessage("Organisation cannot represent both claimant and defendant in the same case");

        verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
        verify(organisationRepository, never()).save(any());
    }

}
