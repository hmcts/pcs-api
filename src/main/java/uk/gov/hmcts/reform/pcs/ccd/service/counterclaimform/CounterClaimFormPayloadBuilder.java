package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CounterClaimFormPayloadBuilder {
    private final CaseReferenceFormatter caseReferenceFormatter;

    public CounterClaimFormPayloadBuilder(CaseReferenceFormatter caseReferenceFormatter) {
        this.caseReferenceFormatter = caseReferenceFormatter;
    }

    public CounterClaimFormPayload build(CounterClaimEntity counterClaim) {
        return CounterClaimFormPayload.builder()
            .referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(
                counterClaim.getPcsCase().getCaseReference()))
            .issueDateSealed(toLocalDate(counterClaim.getClaimIssuedDate()))
            .submittedOn(toLocalDate(counterClaim.getClaimSubmittedDate()))
            .build();
    }

    private static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}
