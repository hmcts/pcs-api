package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.StatementOfTruthEntity;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class StatementOfTruthMapper {

    private final Clock utcClock;

    public StatementOfTruthMapper(Clock utcClock) {
        this.utcClock = utcClock;
    }

    public StatementOfTruthEntity toEntity(StatementOfTruthDetailsEnforcement data) {
        StatementOfTruthEntity.StatementOfTruthEntityBuilder sotBuilder = StatementOfTruthEntity.builder();

        if (data.getCompletedBy() == StatementOfTruthCompletedBy.CLAIMANT) {
            sotBuilder.fullName(data.getFullNameParty());
            sotBuilder.positionHeld(data.getPositionParty());
        } else if (data.getCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            sotBuilder.fullName(data.getFullNameLegalRep());
            sotBuilder.firmName(data.getFirmNameLegalRep());
            sotBuilder.positionHeld(data.getPositionLegalRep());
        }

        sotBuilder.accepted(YesOrNo.YES);
        sotBuilder.completedDate(LocalDateTime.now(utcClock));
        sotBuilder.completedBy(data.getCompletedBy());

        return sotBuilder.build();
    }
}
