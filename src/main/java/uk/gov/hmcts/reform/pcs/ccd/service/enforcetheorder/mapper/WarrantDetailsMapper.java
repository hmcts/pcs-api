package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;

@Component
@AllArgsConstructor
public class WarrantDetailsMapper {

    public WarrantEntity toEntity(EnforcementOrder enforcementOrder,
                                  EnforcementOrderEntity enforcementOrderEntity) {
        WarrantEntity warrantEntity = WarrantEntity.builder()
            .enforcementOrder(enforcementOrderEntity).build();
        if (enforcementOrder.getWarrantDetails() != null) {
            WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
            warrantEntity.setLanguageUsed(warrantDetails.getLanguageUsed());
            breathingSpace(warrantEntity, warrantDetails);
            missedPayments(warrantEntity, warrantDetails);
            additionalInformation(warrantDetails, warrantEntity);
            nameAndAddressForEviction(warrantDetails, warrantEntity);
            propertyAccessDetails(warrantDetails, warrantEntity);
            defendantsDOB(warrantEntity, warrantDetails);
        }
        return warrantEntity;
    }

    private void controlFlags(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setShowPeopleWhoWillBeEvictedPage(
            convertYesOrNoToVerticalYesNo(warrantDetails.getShowPeopleWhoWillBeEvictedPage()));
        warrantEntity.setShowPeopleYouWantToEvictPage(
            convertYesOrNoToVerticalYesNo(warrantDetails.getShowPeopleYouWantToEvictPage()));
    }

    private void suspendTheOrder(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setIsSuspendedOrder(warrantDetails.getIsSuspendedOrder());
    }

    private void breathingSpace(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setDefendantInBreathingSpace(warrantDetails.getDefendantInBreathingSpace());
    }

    private void missedPayments(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setDefendantMissedPayments(warrantDetails.getDefendantMissedPayments());
    }

    private void nameAndAddressForEviction(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getNameAndAddressForEviction() != null) {
            warrantEntity.setCorrectNameAndAddress(
                warrantDetails.getNameAndAddressForEviction().getCorrectNameAndAddress());
        }
    }

    private void additionalInformation(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getAdditionalInformation() != null) {
            warrantEntity.setAdditionalInformationSelect(
                warrantDetails.getAdditionalInformation().getAdditionalInformationSelect());
            warrantEntity.setAdditionalInformationDetails(
                warrantDetails.getAdditionalInformation().getAdditionalInformationDetails());
        }
    }

    private void propertyAccessDetails(WarrantDetails warrantDetails, WarrantEntity warrantEntity) {
        if (warrantDetails.getPropertyAccessDetails() != null) {
            warrantEntity.setIsDifficultToAccessProperty(
                warrantDetails.getPropertyAccessDetails().getIsDifficultToAccessProperty());
            warrantEntity.setClarificationOnAccessDifficultyText(
                warrantDetails.getPropertyAccessDetails().getClarificationOnAccessDifficultyText());
        }
    }

    private void defendantsDOB(WarrantEntity warrantEntity, WarrantDetails warrantDetails) {
        warrantEntity.setDefendantsDOBKnown(warrantDetails.getDefendantsDOBKnown());
        if (warrantDetails.getDefendantsDOB() != null) {
            warrantEntity.setDefendantsDOBDetails(
                warrantDetails.getDefendantsDOB().getDefendantsDOBDetails());
        }
    }

    private VerticalYesNo convertYesOrNoToVerticalYesNo(YesOrNo yesOrNo) {
        return yesOrNo == YesOrNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

}
