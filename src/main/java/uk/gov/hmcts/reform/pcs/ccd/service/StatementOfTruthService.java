package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.StatementOfTruth;

import java.util.List;

@Service
public class StatementOfTruthService {

    public StatementOfTruth buildStatementOfTruth(PCSCase pcsCase) {
        if (pcsCase.getStatementOfTruth() == null
            || pcsCase.getStatementOfTruth().getCompletedBy() == null) {
            return null;
        }

        StatementOfTruthDetails details = pcsCase.getStatementOfTruth();
        StatementOfTruthCompletedBy completedBy = details.getCompletedBy();
        StatementOfTruth.StatementOfTruthBuilder builder = StatementOfTruth.builder()
            .completedBy(completedBy);

        if (completedBy == StatementOfTruthCompletedBy.CLAIMANT) {
            List<StatementOfTruthAgreementClaimant> agreementClaimantList = details.getAgreementClaimant();
            builder.agreementClaimant(
                    agreementClaimantList != null && !agreementClaimantList.isEmpty()
                        ? agreementClaimantList.getFirst()
                        : null)
                .fullNameClaimant(details.getFullNameClaimant())
                .positionClaimant(details.getPositionClaimant());
        } else if (completedBy == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            List<StatementOfTruthAgreementLegalRep> agreementLegalRepList = details.getAgreementLegalRep();
            builder.agreementLegalRep(
                    agreementLegalRepList != null && !agreementLegalRepList.isEmpty()
                        ? agreementLegalRepList.getFirst()
                        : null)
                .fullNameLegalRep(details.getFullNameLegalRep())
                .firmNameLegalRep(details.getFirmNameLegalRep())
                .positionLegalRep(details.getPositionLegalRep());
        }

        return builder.build();
    }

}



