package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyString;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativePartyLinkServiceTest {

    @InjectMocks
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    @Mock
    private IdamService idamService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @Mock
    private LegalRepresentativeRepository legalRepresentativeRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private User user;

    @Mock
    private UserInfo userInfo;

    @Mock
    private AddressUK addressUK;

    @Mock
    private AddressEntity addressEntity;

    @Captor
    private ArgumentCaptor<LegalRepresentativeEntity> legalRepresentativeEntityCaptor;

    @Test
    void linkLegalRepresentativeToParty_WithPartyFound_SavesLegalRepresentativeEntity() {
        // given
        String authToken = "authToken";
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


        when(idamService.validateAuthToken(authToken)).thenReturn(user);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(organisationDetailsService.getOrganisationName(userUid.toString())).thenReturn(organisationName);
        when(userInfo.getName()).thenReturn(name);
        when(userInfo.getFamilyName()).thenReturn(familyName);
        when(userInfo.getSub()).thenReturn(email);
        when(organisationDetailsService.getOrganisationAddress(userUid.toString()))
            .thenReturn(addressUK);
        when(addressMapper.toAddressEntityAndNormalise(addressUK)).thenReturn(addressEntity);

        // when
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(caseReference,
                                                                           authToken,
                                                                           partyId.toString());

        // then
        verify(legalRepresentativeRepository).save(legalRepresentativeEntityCaptor.capture());

        LegalRepresentativeEntity actual = legalRepresentativeEntityCaptor.getValue();

        assertEquals(organisationName, actual.getOrganisationName());
        assertEquals(userUid, actual.getIdamId());
        assertEquals(name, actual.getFirstName());
        assertEquals(familyName, actual.getLastName());
        assertEquals(email, actual.getEmail());
        assertEquals(addressEntity, actual.getAddress());
        assertEquals(partyEntity, actual.getClaimPartyLegalRepresentativeList().getFirst().getParty());
    }

    @Test
    void linkLegalRepresentativeToParty_WithNoPartyFound_ThrowsException() {
        // given
        String authToken = "authToken";
        UUID userUid = UUID.randomUUID();
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


        when(idamService.validateAuthToken(authToken)).thenReturn(user);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        try {
            // when
            legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
                caseReference,
                authToken,
                partyId.toString()
            );
        } catch (PartyNotFoundException e) {
            // then
            assertEquals("Unable to find Party with Id [" + partyId + "]", e.getMessage());

            verify(organisationDetailsService, never()).getOrganisationName(anyString());
            verify(userInfo, never()).getName();
            verify(userInfo, never()).getFamilyName();
            verify(userInfo, never()).getSub();
            verify(organisationDetailsService, never()).getOrganisationAddress(anyString());
            verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
            verify(legalRepresentativeRepository, never()).save(any());
        }
    }

    @Test
    void linkLegalRepresentativeToParty_WithNoDefendantPartyFound_ThrowsException() {
        // given
        String authToken = "authToken";
        UUID userUid = UUID.randomUUID();
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


        when(idamService.validateAuthToken(authToken)).thenReturn(user);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(userInfo.getUid()).thenReturn(userUid.toString());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        try {
            // when
            legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
                caseReference,
                authToken,
                partyId.toString()
            );
        } catch (PartyNotFoundException e) {
            // then
            assertEquals("Unable to find Party with Id [" + partyId + "]", e.getMessage());

            verify(organisationDetailsService, never()).getOrganisationName(anyString());
            verify(userInfo, never()).getName();
            verify(userInfo, never()).getFamilyName();
            verify(userInfo, never()).getSub();
            verify(organisationDetailsService, never()).getOrganisationAddress(anyString());
            verify(addressMapper, never()).toAddressEntityAndNormalise(any(AddressUK.class));
            verify(legalRepresentativeRepository, never()).save(any());
        }
    }

}
