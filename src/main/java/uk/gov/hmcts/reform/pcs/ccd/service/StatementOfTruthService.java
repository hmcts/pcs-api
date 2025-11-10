package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.model.StatementOfTruth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StatementOfTruthService {

    public StatementOfTruth buildStatementOfTruth(PCSCase pcsCase) {
        if (pcsCase.getStatementOfTruthCompletedBy() == null) {
            return null;
        }

        StatementOfTruth.StatementOfTruthBuilder builder = StatementOfTruth.builder()
            .completedBy(pcsCase.getStatementOfTruthCompletedBy() != null
                ? pcsCase.getStatementOfTruthCompletedBy().name() : null);

        if (pcsCase.getStatementOfTruthCompletedBy() == StatementOfTruthCompletedBy.CLAIMANT) {
            builder.agreementClaimant(mapToLabels(pcsCase.getStatementOfTruthAgreementClaimant()))
                .fullNameClaimant(pcsCase.getStatementOfTruthFullNameClaimant())
                .positionClaimant(pcsCase.getStatementOfTruthPositionClaimant());
        } else if (pcsCase.getStatementOfTruthCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            builder.agreementLegalRep(mapToLabels(pcsCase.getStatementOfTruthAgreementLegalRep()))
                .fullNameLegalRep(pcsCase.getStatementOfTruthFullNameLegalRep())
                .firmNameLegalRep(pcsCase.getStatementOfTruthFirmNameLegalRep())
                .positionLegalRep(pcsCase.getStatementOfTruthPositionLegalRep());
        }

        return builder.build();
    }

    private <T extends HasLabel> List<String> mapToLabels(List<T> items) {
        return Optional.ofNullable(items)
            .orElse(Collections.emptyList())
            .stream()
            .map(HasLabel::getLabel)
            .collect(Collectors.toList());
    }

}



