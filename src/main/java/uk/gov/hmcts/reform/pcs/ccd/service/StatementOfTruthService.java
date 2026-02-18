package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

@Service
public class StatementOfTruthService {

    public StatementOfTruthEntity createStatementOfTruthEntity(PCSCase pcsCase) {
        if (pcsCase.getStatementOfTruth() == null
            || pcsCase.getStatementOfTruth().getCompletedBy() == null) {
            return null;
        }

        StatementOfTruthDetails statementOfTruth = pcsCase.getStatementOfTruth();
        StatementOfTruthCompletedBy completedBy = statementOfTruth.getCompletedBy();
        StatementOfTruthEntity statementOfTruthEntity = new StatementOfTruthEntity();

        statementOfTruthEntity.setCompletedBy(completedBy);

        if (completedBy == StatementOfTruthCompletedBy.CLAIMANT) {
            if (claimantAgreed(statementOfTruth)) {
                statementOfTruthEntity.setAccepted(YesOrNo.YES);
            }

            statementOfTruthEntity.setFullName(statementOfTruth.getFullNameClaimant());
            statementOfTruthEntity.setPositionHeld(statementOfTruth.getPositionClaimant());

        } else if (completedBy == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            if (legalRepAgreed(statementOfTruth)) {
                statementOfTruthEntity.setAccepted(YesOrNo.YES);
            }

            statementOfTruthEntity.setFullName(statementOfTruth.getFullNameLegalRep());
            statementOfTruthEntity.setFirmName(statementOfTruth.getFirmNameLegalRep());
            statementOfTruthEntity.setPositionHeld(statementOfTruth.getPositionLegalRep());
        }

        return statementOfTruthEntity;
    }

    private static boolean claimantAgreed(StatementOfTruthDetails statementOfTruth) {
        return statementOfTruth.getAgreementClaimant().stream()
            .findFirst()
            .map(StatementOfTruthAgreementClaimant.BELIEVE_TRUE::equals)
            .orElse(false);
    }

    private static boolean legalRepAgreed(StatementOfTruthDetails statementOfTruth) {
        return statementOfTruth.getAgreementLegalRep().stream()
            .findFirst()
            .map(StatementOfTruthAgreementLegalRep.AGREED::equals)
            .orElse(false);
    }
}

