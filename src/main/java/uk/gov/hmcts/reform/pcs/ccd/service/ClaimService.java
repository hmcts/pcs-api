package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.utils.YesOrNoToBoolean;

import java.util.List;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimGroundService claimGroundService;

    public ClaimEntity createMainClaimEntity(PCSCase pcsCase, PartyEntity claimantPartyEntity) {

        String additionalReasons = pcsCase.getAdditionalReasonsForPossession().getReasons();

        List<ClaimGroundEntity> claimGrounds = claimGroundService.getGroundsWithReason(pcsCase);
        DefendantCircumstances defendantCircumstances = pcsCase.getDefendantCircumstances();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .summary("Main Claim")
            .defendantCircumstances(defendantCircumstances != null
                                        ? defendantCircumstances.getDefendantCircumstancesInfo() : null)
            .suspensionOfRightToBuyHousingAct(pcsCase.getSuspensionOfRightToBuy() != null
                                                  ? pcsCase.getSuspensionOfRightToBuy().getSuspensionOfRightToBuyHousingActs() : null)
            .suspensionOfRightToBuyReason(pcsCase.getSuspensionOfRightToBuy() != null
                                              ? pcsCase.getSuspensionOfRightToBuy().getSuspensionOfRightToBuyReason() : null)
            .demotionOfTenancyHousingAct(pcsCase.getDemotionOfTenancy() != null
                                             ? pcsCase.getDemotionOfTenancy().getDemotionOfTenancyHousingActs() : null)
            .demotionOfTenancyReason(pcsCase.getDemotionOfTenancy() != null
                                         ? pcsCase.getDemotionOfTenancy().getDemotionOfTenancyReason() : null)
            .costsClaimed(pcsCase.getClaimingCostsWanted().toBoolean())
            .additionalReasons(additionalReasons)
            .applicationWithClaim(YesOrNoToBoolean.convert(pcsCase.getApplicationWithClaim()))
            .build();

        claimEntity.addParty(claimantPartyEntity, PartyRole.CLAIMANT);
        claimEntity.addClaimGrounds(claimGrounds);

        claimRepository.save(claimEntity);

        return claimEntity;
    }

}
