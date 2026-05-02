package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyViewTest {

    @Mock
    private ModelMapper modelMapper;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;

    private PartyView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));

        underTest = new PartyView(modelMapper);
    }

    @Test
    void shouldMapPartyEntity() {
        // Given
        PartyEntity partyEntity = mock(PartyEntity.class);
        when(pcsCaseEntity.getParties()).thenReturn(Set.of(partyEntity));

        Party party = mock(Party.class);
        when(modelMapper.map(partyEntity, Party.class)).thenReturn(party);

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<Party>> mappedParties = pcsCase.getParties();
        assertThat(mappedParties).hasSize(1);
        assertThat(mappedParties.getFirst().getValue()).isSameAs(party);
    }

    @Test
    void shouldMapAllPartiesByType() {
        // Given
        Party claimant = mock(Party.class);
        UUID claimantId = UUID.randomUUID();
        ClaimPartyEntity claimantClaimParty = createClaimPartyEntity(claimant, claimantId, PartyRole.CLAIMANT);

        Party defendant1 = mock(Party.class);
        UUID defendant1Id = UUID.randomUUID();
        ClaimPartyEntity defendant1ClaimParty = createClaimPartyEntity(defendant1, defendant1Id, PartyRole.DEFENDANT);

        Party defendant2 = mock(Party.class);
        UUID defendant2Id = UUID.randomUUID();
        ClaimPartyEntity defendant2ClaimParty = createClaimPartyEntity(defendant2, defendant2Id, PartyRole.DEFENDANT);

        Party underlessee1 = mock(Party.class);
        UUID underlessee1Id = UUID.randomUUID();
        ClaimPartyEntity underlessee1ClaimParty = createClaimPartyEntity(
            underlessee1,
            underlessee1Id,
            PartyRole.UNDERLESSEE_OR_MORTGAGEE
        );

        Party underlessee2 = mock(Party.class);
        UUID underlessee2Id = UUID.randomUUID();
        ClaimPartyEntity underlessee2ClaimParty = createClaimPartyEntity(
            underlessee2,
            underlessee2Id,
            PartyRole.UNDERLESSEE_OR_MORTGAGEE
        );

        when(claimEntity.getClaimParties()).thenReturn(
            List.of(claimantClaimParty, defendant1ClaimParty, defendant2ClaimParty,
                    underlessee1ClaimParty, underlessee2ClaimParty
            ));

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getAllClaimants())
            .containsExactly(asListValue(claimantId, claimant));

        assertThat(pcsCase.getAllDefendants())
            .containsExactly(
                asListValue(defendant1Id, defendant1),
                asListValue(defendant2Id, defendant2)
            );

        assertThat(pcsCase.getAllUnderlesseeOrMortgagees())
            .containsExactly(
                asListValue(underlessee1Id, underlessee1),
                asListValue(underlessee2Id, underlessee2)
            );

    }

    private ClaimPartyEntity createClaimPartyEntity(Party party, UUID partyId, PartyRole partyRole) {
        PartyEntity partyEntity = mock(PartyEntity.class);

        when(modelMapper.map(partyEntity, Party.class)).thenReturn(party);

        ClaimPartyId claimPartyId = new ClaimPartyId();
        claimPartyId.setPartyId(partyId);

        return ClaimPartyEntity.builder()
            .id(claimPartyId)
            .role(partyRole)
            .party(partyEntity)
            .build();
    }

    private static ListValue<Party> asListValue(UUID id, Party party) {
        return ListValue.<Party>builder().id(id.toString()).value(party).build();
    }

}
