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
        StatementOfTruthEntity statementOfTruthEntity = new StatementOfTruthEntity();

        if (data.getCompletedBy() == StatementOfTruthCompletedBy.CLAIMANT) {
            statementOfTruthEntity.setFullName(data.getFullNameParty());
            statementOfTruthEntity.setPositionHeld(data.getPositionParty());
        } else if (data.getCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            statementOfTruthEntity.setFullName(data.getFullNameLegalRep());
            statementOfTruthEntity.setFirmName(data.getFirmNameLegalRep());
            statementOfTruthEntity.setPositionHeld(data.getPositionLegalRep());
        }

        statementOfTruthEntity.setAccepted(YesOrNo.YES);
        statementOfTruthEntity.setCompletedDate(LocalDateTime.now(utcClock));
        statementOfTruthEntity.setCompletedBy(data.getCompletedBy());

        return statementOfTruthEntity;
    }
}
