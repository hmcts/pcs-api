package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;

@Component
@AllArgsConstructor
public class WritDetailsMapper {

    public WritEntity toEntity(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        WritEntity entity = WritEntity.builder()
                .enfApplication(enforcementOrderEntity).build();
        if (enforcementOrder.getWritDetails() != null) {
            WritDetails writDetails = enforcementOrder.getWritDetails();
            mapNameAndAddressForEviction(writDetails.getNameAndAddressForEviction(), entity);
            entity.setHasHiredHighCourtEnforcementOfficer(
                    convertToVerticalYesNo(writDetails.getHasHiredHighCourtEnforcementOfficer()));
            entity.setHceoDetails(writDetails.getHceoDetails());
            entity.setHasClaimTransferredToHighCourt(convertYesOrNo(writDetails.getHasClaimTransferredToHighCourt()));
            entity.setLanguageUsed(writDetails.getLanguageUsed());

            breathingSpace(entity, writDetails);
            missedPayments(entity, writDetails);
        }
        return entity;
    }

    private void breathingSpace(WritEntity writEntity, WritDetails writDetails) {
        writEntity.setDefendantInBreathingSpace(writDetails.getDefendantInBreathingSpace());
    }

    private void missedPayments(WritEntity writEntity, WritDetails writDetails) {
        writEntity.setDefendantMissedPayments(writDetails.getDefendantMissedPayments());
    }

    private void mapNameAndAddressForEviction(NameAndAddressForEviction nameAndAddress,
                                              WritEntity entity) {
        if (nameAndAddress != null) {
            entity.setCorrectNameAndAddress(nameAndAddress.getCorrectNameAndAddress());
        }
    }

    private YesOrNo convertYesOrNo(YesOrNo yesOrNo) {
        return yesOrNo == YesOrNo.YES ? YesOrNo.YES : YesOrNo.NO;
    }

    private VerticalYesNo convertToVerticalYesNo(VerticalYesNo yesOrNo) {
        return yesOrNo == VerticalYesNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

}
