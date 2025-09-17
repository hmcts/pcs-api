package uk.gov.hmcts.reform.pcs.ccd.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

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

        List<ClaimGroundEntity> entities = new ArrayList<>();

        for (IntroductoryDemotedOrOtherGrounds ground : introductoryDemotedOrOtherGrounds) {
            String reasonText = switch (ground) {
                case ABSOLUTE_GROUNDS ->
                    pcsCase.getIntroductoryDemotedOtherGroundReason().getAbsoluteGrounds();
                case ANTI_SOCIAL ->
                    pcsCase.getIntroductoryDemotedOtherGroundReason().getAntiSocialBehaviourGround();
                case BREACH_OF_THE_TENANCY ->
                    pcsCase.getIntroductoryDemotedOtherGroundReason().getBreachOfTenancyGround();
                case OTHER -> pcsCase.getIntroductoryDemotedOtherGroundReason().getOtherGround();
                case RENT_ARREARS -> null;
            };

            String otherGround = ground.equals(IntroductoryDemotedOrOtherGrounds.OTHER)
                    ? pcsCase.getOtherGroundsOfPossession() : null;

            entities.add(
                ClaimGroundEntity.builder()
                  .groundsId(ground.name())
                  .claimsReasonText(reasonText)
                  .otherGroundDescription(otherGround)
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
