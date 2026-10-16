package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;

@Component
@Slf4j
@AllArgsConstructor
public class WritDetailsMapper {

    public WritEntity toEntity(WritDetails writDetails) {
        WritEntity entity = new WritEntity();

        // Map NameAndAddressForEviction fields
        mapNameAndAddressForEviction(writDetails.getNameAndAddressForEviction(), entity);

        // Map direct fields
        entity.setHasHiredHighCourtEnforcementOfficer(
            convertToVerticalYesNo(writDetails.getHasHiredHighCourtEnforcementOfficer()));
        entity.setHceoDetails(writDetails.getHceoDetails());
        entity.setHasClaimTransferredToHighCourt(convertYesOrNo(writDetails.getHasClaimTransferredToHighCourt()));
        entity.setLanguageUsed(writDetails.getLanguageUsed());

        breathingSpace(entity, writDetails);
        missedPayments(entity, writDetails);
        return entity;
    }


    private void mapNameAndAddressForEviction(NameAndAddressForEviction nameAndAddress,
                                              WritEntity entity) {
        if (nameAndAddress != null) {
            entity.setCorrectNameAndAddress(nameAndAddress.getCorrectNameAndAddress());
        }
    }

    private void breathingSpace(WritEntity writEntity, WritDetails writDetails) {
        writEntity.setDefendantInBreathingSpace(writDetails.getDefendantInBreathingSpace());
    }

    private void missedPayments(WritEntity writEntity, WritDetails writDetails) {
        writEntity.setDefendantMissedPayments(writDetails.getDefendantMissedPayments());
    }

    private YesOrNo convertYesOrNo(YesOrNo yesOrNo) {
        return yesOrNo == YesOrNo.YES ? YesOrNo.YES : YesOrNo.NO;
    }

    private VerticalYesNo convertToVerticalYesNo(VerticalYesNo yesOrNo) {
        return yesOrNo == VerticalYesNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

}
