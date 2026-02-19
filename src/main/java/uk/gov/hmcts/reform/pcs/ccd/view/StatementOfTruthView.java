package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

import java.util.List;
import java.util.Optional;

@Component
public class StatementOfTruthView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getStatementOfTruth)
            .ifPresent(statementOfTruth -> setStatementOfTruthFields(pcsCase, statementOfTruth));
    }

    private void setStatementOfTruthFields(PCSCase pcsCase, StatementOfTruthEntity statementOfTruthEntity) {
        StatementOfTruthDetails statementOfTruth = new StatementOfTruthDetails();

        StatementOfTruthCompletedBy completedBy = statementOfTruthEntity.getCompletedBy();
        statementOfTruth.setCompletedBy(completedBy);

        if (completedBy == StatementOfTruthCompletedBy.CLAIMANT) {
            statementOfTruth.setFullNameClaimant(statementOfTruthEntity.getFullName());
            statementOfTruth.setPositionClaimant(statementOfTruthEntity.getPositionHeld());
            if (statementOfTruthEntity.getAccepted() == YesOrNo.YES) {
                statementOfTruth.setAgreementClaimant(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE));
            }

        } else if (completedBy == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            statementOfTruth.setFullNameLegalRep(statementOfTruthEntity.getFullName());
            statementOfTruth.setPositionLegalRep(statementOfTruthEntity.getPositionHeld());
            statementOfTruth.setFirmNameLegalRep(statementOfTruthEntity.getFirmName());

            if (statementOfTruthEntity.getAccepted() == YesOrNo.YES) {
                statementOfTruth.setAgreementLegalRep(List.of(StatementOfTruthAgreementLegalRep.AGREED));
            }
        }

        pcsCase.setStatementOfTruth(statementOfTruth);
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

}
