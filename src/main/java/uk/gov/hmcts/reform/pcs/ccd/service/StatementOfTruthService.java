package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.StatementOfTruth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class StatementOfTruthService {

    public StatementOfTruth buildStatementOfTruth(PCSCase pcsCase) {
        if (pcsCase.getStatementOfTruth() == null 
            || pcsCase.getStatementOfTruth().getCompletedBy() == null) {
            return null;
        }

        StatementOfTruthDetails details = pcsCase.getStatementOfTruth();
        StatementOfTruth.StatementOfTruthBuilder builder = StatementOfTruth.builder()
            .completedBy(details.getCompletedBy() != null
                ? details.getCompletedBy().name() : null);

        if (details.getCompletedBy() == StatementOfTruthCompletedBy.CLAIMANT) {
            builder.agreementClaimant(mapToFirstName(details.getAgreementClaimant()))
                .fullNameClaimant(details.getFullNameClaimant())
                .positionClaimant(details.getPositionClaimant());
        } else if (details.getCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            builder.agreementLegalRep(mapToFirstName(details.getAgreementLegalRep()))
                .fullNameLegalRep(details.getFullNameLegalRep())
                .firmNameLegalRep(details.getFirmNameLegalRep())
                .positionLegalRep(details.getPositionLegalRep());
        }

        return builder.build();
    }

    private <T extends Enum<T>> String mapToFirstName(List<T> items) {
        return Optional.ofNullable(items)
            .orElse(Collections.emptyList())
            .stream()
            .findFirst()
            .map(Enum::name)
            .orElse(null);
    }

}



