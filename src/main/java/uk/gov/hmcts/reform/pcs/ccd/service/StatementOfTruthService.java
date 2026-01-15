package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthClaimantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthLegalRepDetails;
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
            buildClaimantDetails(details.getClaimantDetails(), builder);
        } else if (completedBy == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            buildLegalRepDetails(details.getLegalRepDetails(), builder);
        }

        return builder.build();
    }

    private void buildClaimantDetails(StatementOfTruthClaimantDetails claimantDetails,
                                      StatementOfTruth.StatementOfTruthBuilder builder) {
        List<StatementOfTruthAgreementClaimant> agreementClaimantList =
            claimantDetails.getAgreementClaimant();
        builder.agreementClaimant(
                agreementClaimantList != null && !agreementClaimantList.isEmpty()
                    ? agreementClaimantList.getFirst()
                    : null)
            .fullNameClaimant(claimantDetails.getFullNameClaimant())
            .positionClaimant(claimantDetails.getPositionClaimant());
    }

    private void buildLegalRepDetails(StatementOfTruthLegalRepDetails legalRepDetails,
                                      StatementOfTruth.StatementOfTruthBuilder builder) {
        List<StatementOfTruthAgreementLegalRep> agreementLegalRepList =
            legalRepDetails.getAgreementLegalRep();
        builder.agreementLegalRep(
                agreementLegalRepList != null && !agreementLegalRepList.isEmpty()
                    ? agreementLegalRepList.getFirst()
                    : null)
            .fullNameLegalRep(legalRepDetails.getFullNameLegalRep())
            .firmNameLegalRep(legalRepDetails.getFirmNameLegalRep())
            .positionLegalRep(legalRepDetails.getPositionLegalRep());
    }

}



