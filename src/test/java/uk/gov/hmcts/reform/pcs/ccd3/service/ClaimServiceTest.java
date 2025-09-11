package uk.gov.hmcts.reform.pcs.ccd3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd3.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd3.repository.ClaimRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private ClaimService claimService;

    @Test
    void shouldCreateMainClaimAndLinkPartyAndCase() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        PartyEntity partyEntity = new PartyEntity();
        String claimName = "Main Claim";

        ClaimEntity claim = claimService.createAndLinkClaim(caseEntity, partyEntity,
                                                            claimName, PartyRole.CLAIMANT);

        assertThat(claim).isNotNull();
        assertThat(claim.getSummary()).isEqualTo(claimName);
        assertThat(claim.getPcsCase()).isSameAs(caseEntity);
        assertThat(caseEntity.getClaims().iterator().next()).isEqualTo(claim);
        assertThat(claim.getClaimParties().iterator().next().getParty()).isEqualTo(partyEntity);
    }

    @Test
    void shouldSaveClaim() {
        ClaimEntity claim = new ClaimEntity();
        claim.setSummary("Main claim");

        when(claimRepository.save(claim)).thenReturn(claim);

        ClaimEntity result = claimService.saveClaim(claim);

        verify(claimRepository, times(1)).save(claim);
        assertThat(result).isEqualTo(claim);
    }
}

