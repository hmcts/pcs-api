package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.SigneeStatusType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.StatementOfTruthType;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementClaimantLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class BulkStatementOfTruthMapper {

    private final Clock ukClock;

    public BulkStatementOfTruthMapper(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    /**
     * The statement of truth is only ever signed as agreed. PCS does not store the signed date, so it is
     * checked here (it cannot be in the future) but not carried onto the case.
     */
    public StatementOfTruthDetails map(StatementOfTruthType statement, String path, BulkErrorCollector errors) {
        if (!statement.isAgreed()) {
            errors.add(path + "/Agreed", BulkErrorCollector.NOT_AGREED,
                       "The statement of truth must be agreed");
        }
        if (statement.getSignedDate() != null && statement.getSignedDate().isAfter(LocalDate.now(ukClock))) {
            errors.add(path + "/SignedDate", BulkErrorCollector.INVALID_VALUE,
                       "The statement of truth cannot be signed in the future");
        }
        errors.requiredWithMaxLength(path + "/SignedName", statement.getSignedName(), BulkLimits.SIGNATORY,
                                     "Signed name");
        errors.requiredWithMaxLength(path + "/Position", statement.getPosition(), BulkLimits.SIGNATORY,
                                     "Position or office held");

        if (statement.getSigneeStatus() == SigneeStatusType.LEGAL_REPRESENTATIVE) {
            errors.requiredWithMaxLength(path + "/FirmName", statement.getFirmName(), BulkLimits.SIGNATORY,
                                         "Name of firm");
            return StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
                .agreementClaimantLegalRep(List.of(AgreementClaimantLegalRep.AGREED))
                .fullNameLegalRep(statement.getSignedName())
                .firmNameLegalRep(statement.getFirmName())
                .positionLegalRep(statement.getPosition())
                .build();
        }

        return StatementOfTruthDetails.builder()
            .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
            .agreementClaimant(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE))
            .fullNameParty(statement.getSignedName())
            .positionParty(statement.getPosition())
            .build();
    }
}
