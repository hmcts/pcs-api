package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.StatementOfTruth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            builder.agreementClaimant(mapToNames(details.getAgreementClaimant()))
                .fullNameClaimant(details.getFullNameClaimant())
                .positionClaimant(details.getPositionClaimant());
        } else if (details.getCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            builder.agreementLegalRep(mapToNames(details.getAgreementLegalRep()))
                .fullNameLegalRep(details.getFullNameLegalRep())
                .firmNameLegalRep(details.getFirmNameLegalRep())
                .positionLegalRep(details.getPositionLegalRep());
        }

        return builder.build();
    }

    private <T extends Enum<T>> List<String> mapToNames(List<T> items) {
        return Optional.ofNullable(items)
            .orElse(Collections.emptyList())
            .stream()
            .map(Enum::name)
            .collect(Collectors.toList());
    }

}



