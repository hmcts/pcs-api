package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private ClaimService claimService;

    @Captor
    private ArgumentCaptor<ClaimEntity> claimCaptor;

    @Test
    void shouldCreateMainClaimAndLinkPartyAndCase() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        PartyEntity partyEntity = new PartyEntity();
        String claimName = "Main Claim";
        List<ClaimGroundEntity> claimGroundEntities = new ArrayList<>();

        claimService.createAndLinkClaim(
            caseEntity, partyEntity, claimName, PartyRole.CLAIMANT, claimGroundEntities);

        verify(claimRepository).save(claimCaptor.capture());
        ClaimEntity savedEntity = claimCaptor.getValue();

        assertThat(savedEntity.getSummary()).isEqualTo(claimName);
        assertThat(savedEntity.getPcsCase()).isSameAs(caseEntity);
        assertThat(savedEntity.getClaimParties().iterator().next().getParty()).isEqualTo(partyEntity);
        assertThat(savedEntity.getClaimGrounds().isEmpty());
    }

}

