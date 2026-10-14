package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementClaimantLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.StatementOfTruthMapper;

@Service
@RequiredArgsConstructor
public class StatementOfTruthService {

    private final StatementOfTruthMapper statementOfTruthMapper;
    private final StatementOfTruthRepository statementOfTruthRepository;

    @Transactional
    public void processStatementOfTruth(StatementOfTruthDetailsEnforcement statementOfTruthDetailsEnforcement,
                                        EnforcementOrderEntity enforcementOrderEntity) {

        StatementOfTruthEntity statementOfTruthEntity =
                statementOfTruthMapper.toEntity(statementOfTruthDetailsEnforcement);
        enforcementOrderEntity.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruthEntity));
    }

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

            statementOfTruthEntity.setFullName(statementOfTruth.getFullNameParty());
            statementOfTruthEntity.setPositionHeld(statementOfTruth.getPositionParty());

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
        return statementOfTruth.getAgreementClaimantLegalRep().stream()
            .findFirst()
            .map(AgreementClaimantLegalRep.AGREED::equals)
            .orElse(false);
    }
}
