package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        claim.setClaimantCircumstances("Some circumstances");

        when(claimRepository.save(claim)).thenReturn(claim);

        ClaimEntity result = claimService.saveClaim(claim);

        verify(claimRepository, times(1)).save(claim);
        assertThat(result).isEqualTo(claim);
    }

}

