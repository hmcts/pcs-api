package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;

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

        ClaimEntity claim = claimService.createAndLinkClaim(caseEntity, partyEntity,
                                                            ClaimType.MAIN_CLAIM, PartyRole.CLAIMANT);

        assertThat(claim).isNotNull();
        assertThat(claim.getType()).isEqualTo(ClaimType.MAIN_CLAIM);
        assertThat(claim.getPcsCase()).isSameAs(caseEntity);
        assertThat(caseEntity.getClaims().iterator().next()).isEqualTo(claim);
        assertThat(claim.getClaimParties().iterator().next().getParty()).isEqualTo(partyEntity);
    }

    @Test
    void shouldSaveClaim() {
        ClaimEntity claim = new ClaimEntity();
        claim.setType(ClaimType.MAIN_CLAIM);

        when(claimRepository.save(claim)).thenReturn(claim);

        ClaimEntity result = claimService.saveClaim(claim);

        verify(claimRepository, times(1)).save(claim);
        assertThat(result).isEqualTo(claim);
    }
}

