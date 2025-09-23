package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS;

@Service
public class ClaimGroundService {

    public List<ClaimGroundEntity> getGroundsWithReason(PCSCase pcsCase) {
        TenancyLicenceType tenancyLicenceType = pcsCase.getTypeOfTenancyLicence();

        return switch (tenancyLicenceType) {
            case ASSURED_TENANCY -> getAssuredTenancyGroundsWithReason(pcsCase);
            case INTRODUCTORY_TENANCY, DEMOTED_TENANCY, OTHER ->
                getIntroductoryDemotedOtherTenancyGroundsWithReason(pcsCase);
            case SECURE_TENANCY, FLEXIBLE_TENANCY ->
                getSecureFlexibleTenancyGroundsWithReason(pcsCase);
        };
    }

    private List<ClaimGroundEntity> getIntroductoryDemotedOtherTenancyGroundsWithReason(
          PCSCase pcsCase) {
        Set<IntroductoryDemotedOrOtherGrounds> introductoryDemotedOrOtherGrounds =
            pcsCase.getIntroductoryDemotedOrOtherGrounds();

        IntroductoryDemotedOtherGroundReason reasons = pcsCase.getIntroductoryDemotedOtherGroundReason();

        List<ClaimGroundEntity> entities = new ArrayList<>();
        if (introductoryDemotedOrOtherGrounds != null) {
            for (IntroductoryDemotedOrOtherGrounds ground : introductoryDemotedOrOtherGrounds) {
                String reasonText = switch (ground) {
                    case ABSOLUTE_GROUNDS -> reasons.getAbsoluteGrounds();
                    case ANTI_SOCIAL -> reasons.getAntiSocialBehaviourGround();
                    case BREACH_OF_THE_TENANCY -> reasons.getBreachOfTheTenancyGround();
                    case OTHER -> reasons.getOtherGround();
                    case RENT_ARREARS -> null;
                };

                String groundDescription = ground.equals(IntroductoryDemotedOrOtherGrounds.OTHER)
                        ? pcsCase.getOtherGroundDescription() : null;

                entities.add(
                        ClaimGroundEntity.builder()
                                .groundId(ground.name())
                                .groundReason(reasonText)
                                .groundDescription(groundDescription)
                                .build());
            }
        }
        if (pcsCase.getHasIntroductoryDemotedOtherGroundsForPossession() == VerticalYesNo.NO
            && isNotBlank(reasons.getAbsoluteGrounds())) {

            entities.add(
                ClaimGroundEntity.builder()
                    .groundId(ABSOLUTE_GROUNDS.name())
                    .groundReason(pcsCase.getIntroductoryDemotedOtherGroundReason().getAbsoluteGrounds())
                    .groundDescription(null)
                    .build());
        }

        return entities;
    }

    //TODO - Once 1543 is merged refactor and integrate
    private List<ClaimGroundEntity> getAssuredTenancyGroundsWithReason(PCSCase pcsCase) {
        return List.of();
    }

    private List<ClaimGroundEntity> getSecureFlexibleTenancyGroundsWithReason(PCSCase pcsCase) {
        return List.of();
    }
}
